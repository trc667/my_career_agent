package com.example.aimaster.rag;

import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.store.ByteBuffersDirectory;
import org.apache.lucene.store.Directory;
import org.springframework.core.io.Resource;
import org.wltea.analyzer.lucene.IKAnalyzer;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * BM25 稀疏检索器：用 Lucene 对知识库文本构建内存索引，提供关键词精确/模糊匹配检索。
 * <p>
 * 设计（面试可讲）：
 * 1) 为什么用 BM25 补向量检索：向量检索对"语义近义词"强（如"笔试"→"在线笔试"），
 *    但对"专有名词/数字/代码符号"弱（如 "Kafka"、"B+树"、"0.3" 被向量化后语义稀释）；
 *    BM25 基于词频-逆文档频率（TF-IDF 升级版），专有名词命中精准。
 * 2) 中文分词：Lucene 自带 CJKAnalyzer（按双字 bigram 切分，无需外部分词器），
 *    避免 PG 原生全文检索对中文支持差、Windows 装 zhparser 需编译的坑。
 * 3) 内存索引：ByteBuffersDirectory（Lucene 官方推荐的内存目录），
 *    知识库规模 228 段时索引构建毫秒级，评估/调试零外部依赖。
 * 4) 与 pgvector 的混合：向量检索结果 + BM25 检索结果 → RRF（Reciprocal Rank Fusion）
 *    融合排序，兼顾语义召回与精确命中，详见评估器 compareHybrid。
 */
@Slf4j
public class Bm25Retriever {

    private static final String FIELD_CONTENT = "content";
    private static final String FIELD_ID = "id";

    private volatile IndexSearcher searcher;
    private final Analyzer analyzer;
    private volatile int docCount;

    /**
     * 默认分词器：IK(ik_smart) 词语分词（经 327 QA 实测：Hybrid R@5 89.6%→91.7%、MRR 0.747→0.754）。
     * 历史对比：CJK bigram 61.5% R@1 / 82.0% R@5 → IK 65.7% / 84.7%。
     */
    public Bm25Retriever(Resource resource) {
        this(resource, new IKAnalyzer(true));
    }

    /** 直接以知识段列表构建索引（知识库管理入口重建时用，替代读文件） */
    public Bm25Retriever(List<String> paragraphs) {
        this.analyzer = new IKAnalyzer(true);
        rebuild(paragraphs);
    }

    /**
     * 可注入自定义中文分词器（CJK bigram / SmartChinese / IK 等），用于分词对比实验。
     * 索引与检索共用同一 Analyzer，保证语义一致。
     */
    public Bm25Retriever(Resource resource, Analyzer analyzer) {
        this.analyzer = analyzer;
        try {
            String content = resource.getContentAsString(StandardCharsets.UTF_8);
            rebuild(splitParagraphs(content));
        } catch (Exception e) {
            throw new IllegalStateException("BM25 索引构建失败：" + e.getMessage(), e);
        }
    }

    /** 重建索引（知识库变更后由管理入口调用）：以新段落列表构建内存索引并原子替换 */
    public synchronized void rebuild(List<String> paragraphs) {
        if (paragraphs == null || paragraphs.isEmpty()) {
            log.warn("BM25 重建跳过：知识库为空（保持旧索引不变）");
            return;
        }
        try {
            Directory dir = new ByteBuffersDirectory();
            IndexWriterConfig config = new IndexWriterConfig(analyzer);
            config.setSimilarity(new BM25Similarity());
            try (IndexWriter writer = new IndexWriter(dir, config)) {
                int id = 0;
                for (String p : paragraphs) {
                    org.apache.lucene.document.Document doc = new org.apache.lucene.document.Document();
                    doc.add(new TextField(FIELD_CONTENT, p, Field.Store.YES));
                    doc.add(new StringField(FIELD_ID, String.valueOf(id++), Field.Store.YES));
                    writer.addDocument(doc);
                }
                docCount = paragraphs.size();
                log.info("BM25 索引重建完成：{} 段", docCount);
            }
            IndexSearcher newSearcher = new IndexSearcher(DirectoryReader.open(dir));
            newSearcher.setSimilarity(new BM25Similarity());
            searcher = newSearcher;
        } catch (Exception e) {
            log.warn("BM25 索引重建失败：{}（保持旧索引）", e.getMessage());
        }
    }

    /** 与 RagDocumentLoader 相同的分段逻辑：按双换行切分，过滤空段与 # 注释 */
    private List<String> splitParagraphs(String content) {
        List<String> result = new ArrayList<>();
        for (String part : content.split("\\n\\s*\\n")) {
            String s = part.trim();
            if (s.isEmpty() || s.startsWith("#")) continue;
            result.add(s);
        }
        return result;
    }

    /**
     * BM25 检索：query 用 CJKAnalyzer 解析分词，返回按 BM25 分数降序的 topN 条知识段。
     *
     * @return Spring AI Document 列表（text 为知识段原文，与向量库文档同源，可做 RRF 融合）
     */
    public List<org.springframework.ai.document.Document> search(String query, int topN) {
        if (query == null || query.isBlank()) return List.of();
        try {
            QueryParser parser = new QueryParser(FIELD_CONTENT, analyzer);
            // 放宽为 OR 语义：CJK bigram 分词后任意词命中即召回，避免 AND 导致召回为 0
            parser.setDefaultOperator(QueryParser.Operator.OR);
            Query q = parser.parse(query);
            ScoreDoc[] hits = searcher.search(q, Math.max(topN, 1)).scoreDocs;
            List<org.springframework.ai.document.Document> docs = new ArrayList<>();
            for (ScoreDoc hit : hits) {
                String text = searcher.doc(hit.doc).get(FIELD_CONTENT);
                if (text != null && !text.isBlank()) {
                    docs.add(new org.springframework.ai.document.Document(text));
                }
            }
            return docs;
        } catch (Exception e) {
            log.warn("BM25 检索失败（query={}）：{}", query, e.getMessage());
            return List.of();
        }
    }

    public int getDocCount() {
        return docCount;
    }
}

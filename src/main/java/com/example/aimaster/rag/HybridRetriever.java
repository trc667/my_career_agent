package com.example.aimaster.rag;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 生产链路混合检索器：向量召回 + BM25 稀疏召回 → RRF 融合 →（可选）qwen3-rerank 精排。
 * <p>
 * 完整六流程的检索侧落地（③ 多路召回 + ④ Rerank 精排）：
 * <pre>
 * query → ① 预处理（外部） → ② embedding（VectorStore 内部）
 *       → ③a pgvector 向量 topN  +  ③b Lucene BM25 topN
 *       → RRF 融合（按文本匹配累加分数）
 *       → ④ qwen3-rerank 交叉编码器精排 topK
 * </pre>
 * 参数与评估实验（compareHybrid）保持一致：RRF k=60 等权、rerank 候选 top10。
 * 各环节均带降级：rerank 失败返回融合结果，融合失败退化纯向量。
 */
@Slf4j
@Component
public class HybridRetriever {

    private final VectorStore vectorStore;
    private final Reranker reranker;

    /** 两路各召回条数（送入 RRF 融合） */
    @Value("${app.rag.hybrid.rrf-top-n:10}")
    private int rrfTopN;
    /** RRF 常数 k（平滑排名差异） */
    @Value("${app.rag.hybrid.rrf-k:60}")
    private int rrfK;
    /** 是否启用 qwen3-rerank 精排 */
    @Value("${app.rag.hybrid.rerank-enabled:true}")
    private boolean rerankEnabled;
    /** 送入精排的候选条数 */
    @Value("${app.rag.hybrid.rerank-top-n:10}")
    private int rerankTopN;

    private volatile Bm25Retriever bm25;

    public HybridRetriever(VectorStore vectorStore, Reranker reranker) {
        this.vectorStore = vectorStore;
        this.reranker = reranker;
    }

    /** 启动时从知识库文件构建 BM25 内存索引（629 段，毫秒级） */
    @PostConstruct
    public void init() {
        bm25 = new Bm25Retriever(new ClassPathResource("rag/career-tips.txt"));
    }

    /** 知识库变更后重建 BM25 索引（由 KnowledgeService 全量重建时调用，原子替换引用） */
    public void rebuildBm25(List<String> paragraphs) {
        if (paragraphs == null || paragraphs.isEmpty()) {
            log.warn("BM25 重建跳过：知识库为空（保持旧索引不变）");
            return;
        }
        Bm25Retriever rebuilt = new Bm25Retriever(paragraphs);
        bm25 = rebuilt;
    }

    /**
     * 检索主入口：向量 + BM25 → RRF 融合 → Rerank 精排，返回 topK 文档。
     *
     * @param query 原始用户问题
     * @param topK  最终返回条数
     */
    public List<Document> retrieve(String query, int topK) {
        if (query == null || query.isBlank()) return List.of();
        try {
            // ③a 向量召回（pgvector HNSW）
            List<Document> vTop = vectorStore.similaritySearch(
                    SearchRequest.builder().query(query).topK(rrfTopN).build());
            // ③b 稀疏召回（Lucene BM25 + IK 分词）
            List<Document> bTop = bm25.search(query, rrfTopN);
            // RRF 融合（等权 k=60，按文本匹配）
            List<Document> fused = rrfFuse(vTop, bTop, rrfK, Math.max(rrfTopN, topK));
            if (fused.isEmpty()) return fused;
            // ④ Rerank 精排（可关闭降级）
            if (rerankEnabled) {
                List<Document> candidates = fused.subList(0, Math.min(rerankTopN, fused.size()));
                return reranker.rerank(query, candidates, topK);
            }
            return fused.subList(0, Math.min(topK, fused.size()));
        } catch (Exception e) {
            log.warn("混合检索异常，退化纯向量：{}", e.getMessage());
            return vectorStore.similaritySearch(SearchRequest.builder().query(query).topK(topK).build());
        }
    }

    /** RRF 融合：score(d) = Σ 1/(k + rank_i(d))，同一文档两路都命中时分数累加（按文本匹配） */
    static List<Document> rrfFuse(List<Document> list1, List<Document> list2, int k, int topK) {
        Map<String, Double> scores = new LinkedHashMap<>();
        Map<String, Document> seen = new LinkedHashMap<>();
        indexByText(list1, scores, seen, k);
        indexByText(list2, scores, seen, k);
        return scores.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(topK)
                .map(e -> seen.get(e.getKey()))
                .toList();
    }

    private static void indexByText(List<Document> docs, Map<String, Double> scores,
                                    Map<String, Document> seen, int k) {
        for (int i = 0; i < docs.size(); i++) {
            String text = docs.get(i).getText();
            if (text == null || text.isBlank()) continue;
            scores.merge(text, 1.0 / (k + i + 1), Double::sum);
            seen.putIfAbsent(text, docs.get(i));
        }
    }
}

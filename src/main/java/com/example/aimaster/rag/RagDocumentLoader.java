package com.example.aimaster.rag;

import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.JdbcTemplate;

import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * RAG 文档入库：把知识库文本分块、转向量、写入 VectorStore。
 * 仅提供接口与步骤注释，具体逻辑由你实现。
 *
 * 知识库文件路径：src/main/resources/rag/love-tips.txt（classpath: rag/love-tips.txt）
 */
@Slf4j
public class RagDocumentLoader {

    private final VectorStore vectorStore;
    private final TextSplitter textSplitter;
    /** 向量库专用 JdbcTemplate（PostgreSQL pgvector），用于入库前清空旧数据 */
    private final JdbcTemplate jdbcTemplate;

    public RagDocumentLoader(VectorStore vectorStore,
                             TextSplitter textSplitter,
                             JdbcTemplate jdbcTemplate) {
        this.vectorStore = vectorStore;
        this.textSplitter = textSplitter;
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 从资源加载文本，分块后写入向量库。
     *
     * @param resource 例如 new ClassPathResource("rag/love-tips.txt")
     */
    public void loadAndIndex(Resource resource) {
        // 入库前清空旧数据：防止知识库内容变更/扩充后旧向量残留，
        // 导致检索新旧混杂、重复堆积、topK 被重复内容挤占。
        // TRUNCATE 比 DELETE 快且会重置序列；表由 PgVectorStore 的 initializeSchema(true) 自动创建。
        jdbcTemplate.execute("TRUNCATE TABLE vector_store");

        String contentAsString;
        try {
            contentAsString = resource.getContentAsString(StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.warn("资源读取异常：{}", e.getMessage());
            return;
        }
        // 按双换行拆成多段
        String[] parts = contentAsString.split("\\n\\s*\\n");
        List<String>paragraphs=new ArrayList<>();
        for (int i = 0; i < parts.length; i++) {
            String s=parts[i].trim();
            if (s.isEmpty())continue;
            if (s.startsWith("#")) continue;
            paragraphs.add(s);
        }
        // 2. 将每段文本转成 Document（含自动标签）
        List<Document> documents = new ArrayList<>();
        for (String paragraph : paragraphs) {
            // 长段落按句号二次切分，减少"一坨长文本"匹配不准的问题
            List<String> subParagraphs = splitLongParagraph(paragraph, 300);
            for (String sub : subParagraphs) {
                // 自动打标签：基于内容关键词分类（category 存入 metadata）
                Map<String, Object> metadata = new HashMap<>();
                metadata.put("category", autoTag(sub));
                documents.add(new Document(sub, metadata));
            }
        }

        // 若配置了分块器，再拆小（但带有 metadata 的 Document 支持 splitting）
        if (textSplitter != null && !documents.isEmpty()) {
            documents = textSplitter.apply(documents);
        }
        // DashScope embedding API 单次最多 10 条文本（实测 400：batch size should not be larger than 10），
        // 知识库较大时必须分批写入（留余量）
        int batchSize = 8;
        for (int i = 0; i < documents.size(); i += batchSize) {
            List<Document> batch = documents.subList(i, Math.min(i + batchSize, documents.size()));
            vectorStore.add(batch);
        }
        log.info("RAG 入库完成，共 {} 条", documents.size());
    }

    /** 按句号把超过 maxLen 字符的段落切分成多段（保留完整句子，不截断） */
    static List<String> splitLongParagraph(String text, int maxLen) {
        if (text == null || text.length() <= maxLen) return List.of(text);
        String[] sentences = text.split("(?<=[。；！？])");
        List<String> result = new ArrayList<>();
        StringBuilder buf = new StringBuilder();
        for (String s : sentences) {
            if (buf.length() + s.length() > maxLen && buf.length() > 0) {
                result.add(buf.toString().trim());
                buf.setLength(0);
            }
            buf.append(s);
        }
        if (buf.length() > 0) result.add(buf.toString().trim());
        return result.isEmpty() ? List.of(text) : result;
    }

    /** 基于关键词自动分类（面试可讲：减少无关文档干扰，缩小候选池） */
    static String autoTag(String text) {
        if (text.contains("后端") || text.contains("Java") || text.contains("Spring") || text.contains("MySQL") || text.contains("Redis")) return "后端";
        if (text.contains("前端") || text.contains("Vue") || text.contains("React") || text.contains("CSS")) return "前端";
        if (text.contains("算法") || text.contains("LeetCode") || text.contains("动态规划")) return "算法";
        if (text.contains("面试") || text.contains("自我介绍") || text.contains("反问") || text.contains("群面")) return "面试";
        if (text.contains("简历") || text.contains("投递") || text.contains("秋招") || text.contains("春招") || text.contains("校招")) return "校招流程";
        if (text.contains("实习") || text.contains("转正")) return "实习";
        if (text.contains("考研") || text.contains("读研")) return "升学";
        if (text.contains("开源") || text.contains("GitHub") || text.contains("竞赛") || text.contains("博客")) return "软技能";
        return "综合";
    }

    /** 根据问题检索相关文档，拼成参考上下文字符串。 */
    public String retrieveContext(String question, int topK) {
        if (question==null||question.isBlank()) {
            return"";
        }
        List<Document>docs=vectorStore.similaritySearch(SearchRequest.builder().query(question).topK(topK).build());
        StringBuilder sb=new StringBuilder();
        for (int i = 0; i < docs.size(); i++) {
            String content = docs.get(i).getText();
            if (content==null||content.isBlank())continue;
            if (sb.length()>0) sb.append("\n\n");
            sb.append(content); 
        }
        return sb.toString();
    }
}

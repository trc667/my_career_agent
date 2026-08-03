package com.example.aimaster.rag;

import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.Resource;

import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

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

    public RagDocumentLoader(VectorStore vectorStore,
                             TextSplitter textSplitter) {
        this.vectorStore = vectorStore;
        this.textSplitter = textSplitter;
    }

    /**
     * 从资源加载文本，分块后写入向量库。
     *
     * @param resource 例如 new ClassPathResource("rag/love-tips.txt")
     */
    public void loadAndIndex(Resource resource) {
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
        // 2. 将每段文本转成 Document
        //    例：new Document(paragraph) 或 new Document(paragraph, metadataMap)
        List<Document> documents = new ArrayList<>();
        for (int i = 0; i < paragraphs.size(); i++) {
            documents.add(new Document(paragraphs.get(i)));
        }

        // 若配置了分块器，再拆小
        if (textSplitter != null && !documents.isEmpty()) {
            documents = textSplitter.apply(documents);
        }
        vectorStore.add(documents);
        log.info("RAG 入库完成，共 {} 条", documents.size());
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

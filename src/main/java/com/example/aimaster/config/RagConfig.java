package com.example.aimaster.config;

import com.example.aimaster.rag.RagDocumentLoader;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.transformer.splitter.TextSplitter;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RAG 相关 Bean：VectorStore、TextSplitter、RagDocumentLoader。
 * 启动时 RagInitRunner 会调 RagDocumentLoader.loadAndIndex 把知识库入库。
 * TextSplitter 使用 Spring AI 自带的 {@link TokenTextSplitter}（按 token 切块）。
 */
@Configuration
public class RagConfig {

    @Bean
    public VectorStore vectorStore(EmbeddingModel embeddingModel) {
        return SimpleVectorStore.builder(embeddingModel).build();
    }

    @Bean
    public TextSplitter textSplitter(
            @Value("${app.rag.chunk-size:500}") int chunkSize,
            @Value("${app.rag.min-chunk-size-chars:100}") int minChunkSizeChars,
            @Value("${app.rag.keep-separator:false}") boolean keepSeparator) {
        // Spring AI 自带的 TokenTextSplitter：按 token 数切块（chunkSize 在此为 token 数，非字符数）
        return new TokenTextSplitter(chunkSize, minChunkSizeChars, 5, 10_000, keepSeparator);
    }

    @Bean
    public RagDocumentLoader ragDocumentLoader(VectorStore vectorStore,
                                               TextSplitter textSplitter) {
        return new RagDocumentLoader(vectorStore, textSplitter);
    }
}

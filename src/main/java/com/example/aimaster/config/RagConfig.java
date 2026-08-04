package com.example.aimaster.config;

import com.example.aimaster.rag.RagDocumentLoader;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.transformer.splitter.TextSplitter;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

/**
 * RAG 相关 Bean：VectorStore、TextSplitter、RagDocumentLoader。
 * 启动时 RagInitRunner 会调 RagDocumentLoader.loadAndIndex 把知识库入库。
 * TextSplitter 使用 Spring AI 自带的 {@link TokenTextSplitter}（按 token 切块）。
 *
 * VectorStore 已从内存版 SimpleVectorStore 升级为 PostgreSQL + pgvector
 * （{@link PgVectorStore}）：向量落盘持久化，重启不丢，检索走 SQL 层 `<=>` 余弦距离。
 */
@Configuration
public class RagConfig {

    /**
     * 向量存储：PgVectorStore（请完成实现）。
     *
     * 面试可讲的思路：
     * 1) 为什么换 PgVectorStore：SimpleVectorStore 向量存内存、重启丢失，
     *    每次启动都要重新调 embedding API 向量化；pgvector 落盘 + HNSW 索引，
     *    数据可持久化、规模可扩展。
     * 2) 参数 vectorDataSource：双数据源中指向 PostgreSQL 的那个（千万别用 MySQL 主数据源）。
     *
     * 实现步骤：
     * - 第 1 步：确定向量维度 dimensions
     *   text-embedding-v4 默认输出 1024 维；优先取 embeddingModel.getDimensions()，
     *   若 <= 0 则兜底 1024。建表时 vector(1024) 类型依赖此值，填错会类型不匹配。
     * - 第 2 步：用 PgVectorStore.builder(vectorDataSource, embeddingModel) 链式构建：
     *   .dimensions(dimensions)   // 建表维度
     *   .initializeSchema(true)   // 首次启动自动建 vector_store 表（需库已 CREATE EXTENSION vector）
     *   .build()
     */
    @Bean
    public VectorStore vectorStore(EmbeddingModel embeddingModel,
                                   @Qualifier("vectorDataSource") DataSource vectorDataSource) {
        // 关键：PgVectorStore.builder 接收 JdbcTemplate（内部用它执行 INSERT/SELECT ... <=> ），
        // 所以先用 vectorDataSource（PostgreSQL）包装一个 JdbcTemplate
        int dimensions = embeddingModel.dimensions();
        if (dimensions <= 0) {
            dimensions = 1024;
        }
        JdbcTemplate jdbcTemplate = new JdbcTemplate(vectorDataSource);
        return PgVectorStore.builder(jdbcTemplate, embeddingModel)
                .dimensions(dimensions)
                .initializeSchema(true)
                .build();
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
                                               TextSplitter textSplitter,
                                               @Qualifier("vectorDataSource") DataSource vectorDataSource) {
        // JdbcTemplate 用于入库前 TRUNCATE 清空向量表（vectorDataSource 是 PostgreSQL 向量库数据源）
        return new RagDocumentLoader(vectorStore, textSplitter, new JdbcTemplate(vectorDataSource));
    }
}

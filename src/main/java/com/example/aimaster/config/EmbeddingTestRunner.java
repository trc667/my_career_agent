package com.example.aimaster.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import org.springframework.ai.embedding.EmbeddingModel;

/**
 * 仅在 dev 下运行：调一次 EmbeddingModel，确认 DashScope text-embedding 能调通。
 * 看到日志「Embedding 调通」即表示成功。
 */
@Component
@Profile("dev")
@Order(999)
public class EmbeddingTestRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(EmbeddingTestRunner.class);

    private final EmbeddingModel embeddingModel;

    public EmbeddingTestRunner(EmbeddingModel embeddingModel) {
        this.embeddingModel = embeddingModel;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            float[] vector = embeddingModel.embed("你好");
            int dim = vector == null ? 0 : vector.length;
            log.info("---------- Embedding 调通 ---------- 向量维度: {} ----------", dim);
        } catch (Exception e) {
            log.error("Embedding 调用失败，请检查 application-dev.yml 中 embedding 配置与 API Key", e);
        }
    }
}

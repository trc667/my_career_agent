package com.example.aimaster.rag;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

@Component
// 修复：原 @Profile("dev") 导致生产环境部署时不加载知识库，RAG 静默失效。
// 现在 dev/prod 环境都会在启动时初始化知识库（向量化入库到 pgvector）。
// 面试可讲：Spring @Profile 控制 Bean 装配，不同环境 profile 的行为差异。
@Profile({"dev", "prod"})
@Order(998)
public class RagInitRunner implements ApplicationRunner {

    private final RagDocumentLoader ragDocumentLoader;

    public RagInitRunner(RagDocumentLoader ragDocumentLoader) {
        this.ragDocumentLoader = ragDocumentLoader;
    }

    @Override
    public void run(ApplicationArguments args) {
        Resource resource = new ClassPathResource("rag/career-tips.txt");
        ragDocumentLoader.loadAndIndex(resource);
    }
}

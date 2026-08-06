package com.example.aimaster.rag;

import com.example.aimaster.service.KnowledgeService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
// 修复：原 @Profile("dev") 导致生产环境部署时不加载知识库，RAG 静默失效。
// 现在 dev/prod 环境都会在启动时初始化知识库（向量化入库到 pgvector）。
// 面试可讲：Spring @Profile 控制 Bean 装配，不同环境 profile 的行为差异。
@Profile({"dev", "prod"})
@Order(998)
public class RagInitRunner implements ApplicationRunner {

    private final KnowledgeService knowledgeService;

    public RagInitRunner(KnowledgeService knowledgeService) {
        this.knowledgeService = knowledgeService;
    }

    @Override
    public void run(ApplicationArguments args) {
        // 知识库统一走 DB 事实源：首次从 career-tips.txt 导入，随后基于启用知识段重建
        // pgvector 向量库 + BM25 + 八股内存缓存（原 loadAndIndex 直接读文件已废弃）
        knowledgeService.ensureInitialized();
    }
}

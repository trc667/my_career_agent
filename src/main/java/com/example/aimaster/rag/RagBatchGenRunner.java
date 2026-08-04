package com.example.aimaster.rag;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 知识库批量生成触发入口：仅在 raggen profile 下执行，不影响 dev/prod 正常启动。
 * <p>
 * 运行方式（PowerShell）：
 * <pre>
 * mvn spring-boot:run "-Dspring-boot.run.arguments=--spring.profiles.active=raggen"
 * </pre>
 * 生成完成自动退出进程（System.exit(0)），结果写入 target/generated-knowledge.txt。
 */
@Slf4j
@Component
@Profile("raggen")
public class RagBatchGenRunner implements ApplicationRunner {

    private final KnowledgeBatchGenerator generator;

    @Value("${app.rag-gen.topics:Java 并发编程深入,Spring 框架原理}")
    private List<String> topics;

    @Value("${app.rag-gen.per-topic-count:12}")
    private int perTopicCount;

    @Value("${app.rag-gen.batch-size:6}")
    private int batchSize;

    public RagBatchGenRunner(KnowledgeBatchGenerator generator) {
        this.generator = generator;
    }

    @Override
    public void run(ApplicationArguments args) {
        log.info("========== RAG 知识库批量生成开始：{} ==========", topics);
        generator.generate(topics, perTopicCount, batchSize);
        log.info("========== RAG 知识库批量生成结束，进程退出 ==========");
        System.exit(0);
    }
}

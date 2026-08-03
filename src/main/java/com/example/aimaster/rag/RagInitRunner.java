package com.example.aimaster.rag;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

@Component
@Profile("dev")
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

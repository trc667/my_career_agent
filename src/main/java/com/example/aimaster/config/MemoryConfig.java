package com.example.aimaster.config;

import com.example.aimaster.memory.ConversationMemoryStore;
import com.example.aimaster.memory.FileConversationMemoryStore;
import com.example.aimaster.memory.InMemoryConversationMemoryStore;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 对话记忆存储的装配：根据 app.memory.type 选择实现。
 * <p>
 * - memory：内存，重启丢失。
 * - file：文件，默认，持久化到本地目录。
 */
@Configuration
public class MemoryConfig {

    @Value("${app.memory.type:file}")
    private String memoryType;

    @Value("${app.memory.max-messages-per-conversation:20}")
    private int maxMessagesPerConversation;

    @Value("${app.memory.file.base-dir:./data/chat-memory}")
    private String fileBaseDir;

    @Bean
    public ConversationMemoryStore conversationMemoryStore(ObjectMapper objectMapper) {
        if ("file".equalsIgnoreCase(memoryType)) {
            Path baseDir = Paths.get(fileBaseDir).toAbsolutePath().normalize();
            return new FileConversationMemoryStore(baseDir, objectMapper, maxMessagesPerConversation);
        }
        return new InMemoryConversationMemoryStore(maxMessagesPerConversation);
    }
}

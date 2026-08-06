package com.example.aimaster.config;

import com.example.aimaster.memory.ConversationMemoryStore;
import com.example.aimaster.memory.FileConversationMemoryStore;
import com.example.aimaster.memory.InMemoryConversationMemoryStore;
import com.example.aimaster.mapper.ConversationMessageMapper;
import com.example.aimaster.memory.DbConversationMemoryStore;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 对话记忆存储的装配：根据 app.memory.type 选择实现。
 * <p>
 * - db：MySQL，默认，完整历史落库可跨设备回看。
 * - memory：内存，重启丢失。
 * - file：文件，持久化到本地目录（兼容旧部署）。
 */
@Configuration
public class MemoryConfig {

    @Value("${app.memory.type:db}")
    private String memoryType;

    @Value("${app.memory.max-messages-per-conversation:20}")
    private int maxMessagesPerConversation;

    @Value("${app.memory.file.base-dir:./data/chat-memory}")
    private String fileBaseDir;

    @Bean
    public ConversationMemoryStore conversationMemoryStore(ObjectMapper objectMapper,
                                                           ConversationMessageMapper messageMapper) {
        if ("file".equalsIgnoreCase(memoryType)) {
            Path baseDir = Paths.get(fileBaseDir).toAbsolutePath().normalize();
            return new FileConversationMemoryStore(baseDir, objectMapper, maxMessagesPerConversation);
        }
        if ("memory".equalsIgnoreCase(memoryType)) {
            return new InMemoryConversationMemoryStore(maxMessagesPerConversation);
        }
        return new DbConversationMemoryStore(messageMapper, maxMessagesPerConversation);
    }
}

package com.example.aimaster.dto;

import reactor.core.publisher.Flux;

/**
 * 流式对话会话，用于 chatWithRagStream 返回。
 */
public record ChatStreamSession(String conversationId, Flux<String> flux) {}

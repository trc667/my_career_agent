package com.example.aimaster.service;

import com.example.aimaster.dto.ChatResponse;
import com.example.aimaster.dto.CareerReport;

/**
 * 计算机学生职规大师智能体服务接口（三层架构 - Service 层）。
 */
public interface CareerMasterService {

    /**
     * 单轮对话，无记忆。
     */
    ChatResponse chat(String userMessage);

    /**
     * 多轮对话，带会话 ID：会读历史、拼 Prompt、写回记忆。
     * conversationId 为空时视为新会话，会生成新 ID 并放在响应中返回。
     */
    ChatResponse chat(String conversationId, String userMessage);

    /**
     * 带 RAG 的多轮对话：先检索知识库得到参考上下文，再与历史、当前问题一起组 Prompt 调用模型。
     * model 为可选模型名（空则默认 qwen-plus），按实际 token 消耗积分。
     */
    ChatResponse chatWithRag(String conversationId, String userMessage, String model);

    /**
     * 带 RAG 的流式对话，返回 Flux 逐字输出。
     * model 为可选模型名（空则默认 qwen-plus），按实际 token 消耗积分。
     */
    com.example.aimaster.dto.ChatStreamSession chatWithRagStream(String conversationId, String userMessage, String model);

    /**
     * 根据主题生成职规/学习建议报告（结构化输出 JSON）。
     */
    CareerReport generateReport(String topic);

    /**
     * ReAct 多步规划对话（同步），支持工具调用（高德、联网搜索、PDF、记笔记等）。
     */
    ChatResponse chatWithReAct(String conversationId, String userMessage, int maxSteps);

    /**
     * ReAct 多步规划流式执行：每完成一步（思考/工具调用/工具结果/reply）即通过 stepConsumer 推送 JSON。
     */
    void chatWithReActStream(String conversationId, String userMessage, int maxSteps,
                            java.util.function.Consumer<String> stepConsumer);
}

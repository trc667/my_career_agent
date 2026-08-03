package com.example.aimaster.memory;

import org.springframework.ai.chat.messages.Message;

import java.util.List;

/**
 * 多轮对话记忆存储：按会话 ID 存取历史消息。
 * 实现类可由你自行完成（内存 Map、Redis、DB 等）。
 */
public interface ConversationMemoryStore {

    /**
     * 获取某会话的历史消息列表（按时间顺序，不含系统提示）。
     * 用于拼进 Prompt 时放在 SystemMessage 和当前 UserMessage 之间。
     *
     * @param conversationId 会话 ID
     * @return 历史消息，无则返回空列表
     */
    List<Message> getMessages(String conversationId);

    /**
     * 向某会话追加一条消息（如 UserMessage 或 AssistantMessage）。
     *
     * @param conversationId 会话 ID
     * @param message        消息
     */
    void add(String conversationId, Message message);

    /**
     * 清空某会话的历史（可选能力）。
     *
     * @param conversationId 会话 ID
     */
    void clear(String conversationId);
}

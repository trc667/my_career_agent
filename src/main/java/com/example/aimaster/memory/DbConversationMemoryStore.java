package com.example.aimaster.memory;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.aimaster.entity.ConversationMessage;
import com.example.aimaster.mapper.ConversationMessageMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.UserMessage;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 对话记忆的 MySQL 实现（完整历史落库，跨设备可回看）。
 * <p>
 * - {@code add} 将消息插入 conversation_message 表（全量保留，不物理截断）；
 * - {@code getMessages} 仅返回最近 maxMessagesPerConversation 条（作为 LLM 多轮上下文长度控制）；
 * - {@code clear} 删除该会话全部消息。
 */
public class DbConversationMemoryStore implements ConversationMemoryStore {

    private static final Logger log = LoggerFactory.getLogger(DbConversationMemoryStore.class);

    /** 单会话最多纳入 LLM 上下文的最近消息条数 */
    private final int maxMessagesPerConversation;
    private final ConversationMessageMapper messageMapper;

    public DbConversationMemoryStore(ConversationMessageMapper messageMapper, int maxMessagesPerConversation) {
        this.messageMapper = messageMapper;
        this.maxMessagesPerConversation = maxMessagesPerConversation;
    }

    @Override
    public List<Message> getMessages(String conversationId) {
        if (conversationId == null || conversationId.isBlank()) return Collections.emptyList();
        try {
            // 取最近的 N 条（倒序 LIMIT），再反转恢复时间升序
            List<ConversationMessage> rows = messageMapper.selectList(
                    new LambdaQueryWrapper<ConversationMessage>()
                            .eq(ConversationMessage::getConversationId, conversationId)
                            .orderByDesc(ConversationMessage::getCreateTime)
                            .last("LIMIT " + Math.max(1, maxMessagesPerConversation)));
            Collections.reverse(rows);
            List<Message> result = new ArrayList<>(rows.size());
            for (ConversationMessage row : rows) {
                Message msg = toMessage(row.getRole(), row.getContent());
                if (msg != null) result.add(msg);
            }
            return result;
        } catch (Exception e) {
            log.warn("读取会话消息失败: conversationId={}", conversationId, e);
            return Collections.emptyList();
        }
    }

    @Override
    public void add(String conversationId, Message message) {
        if (conversationId == null || conversationId.isBlank() || message == null) return;
        String role = message.getMessageType() == MessageType.ASSISTANT ? "assistant" : "user";
        ConversationMessage row = ConversationMessage.builder()
                .conversationId(conversationId)
                .role(role)
                .content(getMessageText(message))
                .createTime(LocalDateTime.now())
                .build();
        try {
            messageMapper.insert(row);
        } catch (Exception e) {
            log.warn("写入会话消息失败: conversationId={}", conversationId, e);
        }
    }

    @Override
    public void clear(String conversationId) {
        if (conversationId == null || conversationId.isBlank()) return;
        try {
            messageMapper.delete(new LambdaQueryWrapper<ConversationMessage>()
                    .eq(ConversationMessage::getConversationId, conversationId));
        } catch (Exception e) {
            log.warn("清空会话消息失败: conversationId={}", conversationId, e);
        }
    }

    private static String getMessageText(Message message) {
        if (message == null) return "";
        try {
            String t = message.getText();
            return t != null ? t : "";
        } catch (Exception e) {
            if (message instanceof UserMessage u) return u.getText() != null ? u.getText() : "";
            if (message instanceof AssistantMessage a) return a.getText() != null ? a.getText() : "";
            return "";
        }
    }

    private static Message toMessage(String role, String content) {
        if (role == null) return null;
        String text = content != null ? content : "";
        return switch (role.toLowerCase()) {
            case "user" -> new UserMessage(text);
            case "assistant" -> new AssistantMessage(text);
            default -> null;
        };
    }
}

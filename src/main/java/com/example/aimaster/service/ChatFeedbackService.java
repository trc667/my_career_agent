package com.example.aimaster.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.aimaster.dto.ChatFeedbackRequest;
import com.example.aimaster.entity.ChatFeedback;
import com.example.aimaster.mapper.ChatFeedbackMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 问答反馈服务：点赞/点踩一条 AI 回复。
 * <p>
 * 同一用户对同一消息（message_id）只能一个反馈：重复提交切换类型（up⇄down），
 * 数据沉淀在 chat_feedback 表，供后续 RAG 回答质量分析。
 */
@Service
public class ChatFeedbackService {

    private static final Logger log = LoggerFactory.getLogger(ChatFeedbackService.class);

    private final ChatFeedbackMapper feedbackMapper;

    public ChatFeedbackService(ChatFeedbackMapper feedbackMapper) {
        this.feedbackMapper = feedbackMapper;
    }

    /** 保存/切换反馈（失败只打日志不影响主流程） */
    public void save(Long userId, ChatFeedbackRequest req) {
        try {
            ChatFeedback existing = feedbackMapper.selectOne(new LambdaQueryWrapper<ChatFeedback>()
                    .eq(ChatFeedback::getUserId, userId)
                    .eq(ChatFeedback::getMessageId, req.getMessageId()));
            if (existing != null) {
                existing.setFeedbackType(req.getFeedbackType());
                feedbackMapper.updateById(existing);
                return;
            }
            feedbackMapper.insert(ChatFeedback.builder()
                    .userId(userId)
                    .conversationId(req.getConversationId())
                    .messageId(req.getMessageId())
                    .feedbackType(req.getFeedbackType())
                    .createTime(LocalDateTime.now())
                    .build());
        } catch (Exception e) {
            log.warn("问答反馈保存失败: {}", e.getMessage());
        }
    }
}

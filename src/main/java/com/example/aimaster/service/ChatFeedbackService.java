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
    private final PointService pointService;

    public ChatFeedbackService(ChatFeedbackMapper feedbackMapper, PointService pointService) {
        this.feedbackMapper = feedbackMapper;
        this.pointService = pointService;
    }

    /** 保存/切换反馈（失败只打日志不影响主流程）；首次点赞奖励 2 积分 */
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
            // 首次点赞奖励积分（切换类型不重复发；点赞失败不阻塞主流程）
            if ("up".equals(req.getFeedbackType())) {
                try {
                    pointService.addPoints(userId, 2, "聊天点赞");
                } catch (Exception e) {
                    log.warn("点赞积分发放失败: {}", e.getMessage());
                }
            }
        } catch (Exception e) {
            log.warn("问答反馈保存失败: {}", e.getMessage());
        }
    }
}

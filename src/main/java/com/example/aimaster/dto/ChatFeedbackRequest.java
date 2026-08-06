package com.example.aimaster.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 问答反馈请求：点赞/点踩一条 AI 回复（与意见反馈 FeedbackRequest 区分）。
 */
@Data
public class ChatFeedbackRequest {

    /** 会话 ID */
    @NotBlank(message = "conversationId 不能为空")
    @Size(max = 64, message = "conversationId 长度不超过64")
    private String conversationId;

    /** 前端消息 uuid */
    @NotBlank(message = "messageId 不能为空")
    @Size(max = 64, message = "messageId 长度不超过64")
    private String messageId;

    /** 反馈类型：up / down */
    @NotBlank(message = "feedbackType 不能为空")
    @Pattern(regexp = "up|down", message = "feedbackType 仅支持 up/down")
    private String feedbackType;
}

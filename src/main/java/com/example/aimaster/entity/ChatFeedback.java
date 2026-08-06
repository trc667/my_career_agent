package com.example.aimaster.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 聊天问答反馈：message_id 为前端消息 uuid，同用户同消息唯一（赞/踩可切换），沉淀供 RAG 优化。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("chat_feedback")
public class ChatFeedback {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属用户 ID */
    @TableField("user_id")
    private Long userId;

    /** 会话 ID（conversation_id） */
    @TableField("conversation_id")
    private String conversationId;

    /** 前端消息 uuid（ChatMessage.id） */
    @TableField("message_id")
    private String messageId;

    /** 反馈类型：up / down */
    @TableField("feedback_type")
    private String feedbackType;

    @TableField("created_at")
    private LocalDateTime createTime;
}

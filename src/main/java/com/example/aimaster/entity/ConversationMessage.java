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
 * 对话消息表：聊天历史完整落库（不截断，供跨设备回看）。
 * <p>
 * 注意：给 LLM 的多轮上下文仍由 {@code ConversationMemoryStore#getMessages} 按最近 N 条截断，
 * 该表本身保留全部历史。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("conversation_message")
public class ConversationMessage {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属会话 ID（conversation.conversation_id） */
    @TableField("conversation_id")
    private String conversationId;

    /** 消息角色：user / assistant */
    @TableField("role")
    private String role;

    /** 消息内容 */
    @TableField("content")
    private String content;

    @TableField("created_at")
    private LocalDateTime createTime;
}

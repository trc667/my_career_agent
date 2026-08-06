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
 * 对话会话表：聊天历史跨设备同步的元数据。
 * <p>
 * conversationId 为后端生成的 UUID，同时作为对话记忆（conversation_message）的主键。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("conversation")
public class Conversation {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属用户 ID（app_user.id），用于用户数据隔离 */
    @TableField("user_id")
    private Long userId;

    /** 会话唯一标识（后端生成 UUID），也是消息表的外键 */
    @TableField("conversation_id")
    private String conversationId;

    /** 会话标题（默认「新的职规咨询」，首条用户消息后自动更新） */
    @TableField("title")
    private String title;

    @TableField("created_at")
    private LocalDateTime createTime;

    @TableField("updated_at")
    private LocalDateTime updateTime;
}

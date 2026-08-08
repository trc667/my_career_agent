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
 * 积分流水表：积分变更可审计（谁/何时/为何/变了几）。
 * 签到、聊天点赞、邀请奖励等一切积分变动都写此表。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("point_log")
public class PointLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private Long userId;

    /** 变更积分（正数增加/负数扣减） */
    @TableField("change_points")
    private Integer changePoints;

    /** 原因，如：每日签到 / 聊天点赞 / 邀请奖励 */
    @TableField("reason")
    private String reason;

    @TableField("created_at")
    private LocalDateTime createTime;
}

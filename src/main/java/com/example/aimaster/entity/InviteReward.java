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
 * 邀请奖励表：分享裂变防刷（每个邀请人-被邀人组合只奖励一次，
 * 由 (inviter_id, invitee_id) 唯一键保证）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("invite_reward")
public class InviteReward {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 邀请人 */
    @TableField("inviter_id")
    private Long inviterId;

    /** 被邀人（完成首轮对话后触发奖励） */
    @TableField("invitee_id")
    private Long inviteeId;

    /** 奖励积分 */
    @TableField("points")
    private Integer points;

    @TableField("created_at")
    private LocalDateTime createTime;
}

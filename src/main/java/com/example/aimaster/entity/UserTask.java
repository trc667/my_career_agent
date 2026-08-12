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
 * 新手引导任务领取记录：任务完成状态从业务表实时判定（conversation/sign_in/interview_record/redeem_record），
 * 本表只记录「已领取奖励」，唯一约束保证同一任务只能领取一次（幂等防刷）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("user_task")
public class UserTask {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private Long userId;

    /** 任务标识：first_chat / first_sign / first_interview / first_redeem */
    @TableField("task_key")
    private String taskKey;

    /** 领取的奖励积分数 */
    @TableField("reward_points")
    private Integer rewardPoints;

    @TableField("created_at")
    private LocalDateTime createTime;
}

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
 * 简历评分记录表：每次评分结果入库，支持历史回看与删除。
 * <p>
 * detailJson 存完整评分明细 JSON（维度/亮点/不足/优化版简历），
 * 列表接口不返回该字段（仅详情接口返回）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("resume_review")
public class ResumeReview {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属用户 ID（app_user.id），用于用户数据隔离 */
    @TableField("user_id")
    private Long userId;

    /** 目标岗位（可空） */
    @TableField("target_position")
    private String targetPosition;

    /** 用户提交的简历原文 */
    @TableField("resume_text")
    private String resumeText;

    /** 总分（0-100） */
    @TableField("total_score")
    private Integer totalScore;

    /** 评分明细 JSON（ResumeReviewResult 序列化） */
    @TableField("detail_json")
    private String detailJson;

    @TableField("created_at")
    private LocalDateTime createTime;
}

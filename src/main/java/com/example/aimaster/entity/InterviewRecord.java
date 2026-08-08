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
 * 面试记录：完成一场面试即落库（历史回看/周报/运营看板的数据源）。
 * dimensions_json / items_json 为 JSON 字符串，前端按需解析。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("interview_record")
public class InterviewRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private Long userId;

    /** 面试岗位（后端/前端/算法…） */
    @TableField("position")
    private String position;

    /** 面试总分 */
    @TableField("total_score")
    private Integer totalScore;

    /** 分维度均值 JSON：[{"name":"专业度","score":80}] */
    @TableField("dimensions_json")
    private String dimensionsJson;

    /** 逐题明细 JSON：[{"question":"...","score":78,"comment":"..."}] */
    @TableField("items_json")
    private String itemsJson;

    @TableField("created_at")
    private LocalDateTime createTime;
}

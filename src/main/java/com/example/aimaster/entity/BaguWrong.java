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
 * 八股错题本：同一用户同一题唯一（question_id 为内容 hash），重复答错累计次数。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("bagu_wrong")
public class BaguWrong {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属用户 ID */
    @TableField("user_id")
    private Long userId;

    /** 题目内容 hash（BaguService.hashId） */
    @TableField("question_id")
    private String questionId;

    /** 题目内容（冗余存储，防知识库变动后错题丢失） */
    @TableField("question_content")
    private String questionContent;

    /** 分类（autoTag 结果） */
    @TableField("category")
    private String category;

    /** 累计答错次数 */
    @TableField("wrong_count")
    private Integer wrongCount;

    /** 最近一次答错时间 */
    @TableField("last_wrong_at")
    private LocalDateTime lastWrongAt;

    /** 是否已掌握（1=已掌握，从错题列表隐藏） */
    @TableField("mastered")
    private Integer mastered;

    @TableField("created_at")
    private LocalDateTime createTime;
}

package com.example.aimaster.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 简历评分单维度结果。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResumeDimension {

    /** 维度名称，如「项目经历」「量化成果」 */
    private String name;

    /** 该维度得分（0-100） */
    private Integer score;

    /** 评价（指出现状） */
    private String comment;

    /** 改进建议 */
    private String suggestion;
}

package com.example.aimaster.dto;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 简历评分完整结果（结构化输出），用于 /api/resume/review 接口。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonPropertyOrder({"totalScore", "summary", "dimensions", "highlights", "weaknesses", "improvedResume"})
public class ResumeReviewResult {

    /** 总分（0-100） */
    private Integer totalScore;

    /** 总体评价 */
    private String summary;

    /** 各维度评分明细 */
    private List<ResumeDimension> dimensions;

    /** 简历亮点 */
    private List<String> highlights;

    /** 主要不足 */
    private List<String> weaknesses;

    /** 优化后的简历全文 */
    private String improvedResume;
}

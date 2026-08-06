package com.example.aimaster.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 简历评分请求：粘贴简历文本（必填）+ 目标岗位（可选）。
 */
@Data
public class ResumeReviewRequest {

    /** 简历全文（纯文本） */
    @NotBlank(message = "简历内容不能为空")
    @Size(max = 5000, message = "简历内容过长，请控制在5000字以内")
    private String resumeText;

    /** 目标岗位，例如「Java 后端开发工程师」（可选） */
    @Size(max = 128, message = "目标岗位长度不超过128")
    private String targetPosition;
}

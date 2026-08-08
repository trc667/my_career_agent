package com.example.aimaster.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * AI 面试模拟：作答当前题目请求体。
 */
@Data
public class InterviewAnswerRequest {

    /** 面试会话 ID（start 接口返回） */
    @NotBlank(message = "会话不存在")
    @Size(max = 64, message = "会话不合法")
    private String sessionId;

    /** 用户对当前题目的回答 */
    @NotBlank(message = "回答不能为空")
    @Size(max = 2000, message = "回答不超过2000字")
    private String answer;
}

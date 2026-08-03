package com.example.aimaster.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 意见反馈请求体。
 */
@Data
public class FeedbackRequest {

    /** 联系方式：邮箱/微信号等，可空 */
    @Size(max = 128, message = "联系方式不超过128字")
    private String contact;

    /** 反馈内容（必填） */
    @NotBlank(message = "反馈内容不能为空")
    @Size(max = 2000, message = "反馈内容不超过2000字")
    private String content;
}

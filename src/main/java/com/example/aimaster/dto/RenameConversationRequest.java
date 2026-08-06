package com.example.aimaster.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 重命名会话请求：title 必填。
 */
@Data
public class RenameConversationRequest {

    @NotBlank(message = "标题不能为空")
    @Size(max = 128, message = "标题长度不超过128")
    private String title;
}

package com.example.aimaster.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 创建会话请求：title 可选（默认「新的职规咨询」，首条用户消息后自动更新）。
 */
@Data
public class CreateConversationRequest {

    @Size(max = 128, message = "标题长度不超过128")
    private String title;
}

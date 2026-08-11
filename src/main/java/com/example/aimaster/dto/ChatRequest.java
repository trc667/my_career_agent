package com.example.aimaster.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 与职规大师对话的请求体。
 */
@Data
public class ChatRequest {

    /**
     * 用户输入的问题或描述（必填）。
     */
    @NotBlank(message = "消息内容不能为空")
    @Size(max = 2000, message = "单条消息不超过2000字")
    private String message;

    /**
     * 是否使用流式响应（可选，默认由服务端配置决定）。
     */
    private Boolean stream;

    /**
     * 多轮对话的会话 ID。不传则视为新会话或单轮；传则在该会话历史基础上继续对话。
     */
    private String conversationId;

    /**
     * 可选模型名（如 deepseek-v3 / qwen-max）。空则服务端用默认模型 qwen-plus；
     * 不同模型按实际 token 消耗不同积分（费率见 /api/models）。
     */
    private String model;
}

package com.example.aimaster.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 职规大师回复（非流式）的响应体。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatResponse {

    /** 助手回复的完整文本 */
    private String reply;

    /** 本次对话消耗的 token 数（若模型返回） */
    private Integer usageTokens;

    /** 多轮对话的会话 ID。新会话时可由服务端生成并返回，后续请求带上此 ID 即可延续对话。 */
    private String conversationId;

    /** ReAct 多步规划时的步骤列表（thought/tool_call/tool_result），同步接口返回 */
    private java.util.List<com.example.aimaster.dto.ReActStep> steps;
}

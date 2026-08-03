package com.example.aimaster.dto;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * ReAct 多步规划中的单步（thought / tool_call / tool_result）。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonPropertyOrder({"type", "content", "toolName", "toolInput"})
public class ReActStep {
    private String type;
    private String content;
    private String toolName;
    private String toolInput;
}

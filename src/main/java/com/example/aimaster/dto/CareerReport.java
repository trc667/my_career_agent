package com.example.aimaster.dto;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 职规/学习建议报告（结构化输出），用于 /api/report 接口。
 * 模型按此结构返回 JSON，便于前端展示或导出。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonPropertyOrder({"title", "suggestions", "summary"})
public class CareerReport {

    /** 报告标题 */
    private String title;
    /** 建议或行动项列表，2～5 条 */
    private List<String> suggestions;
    /** 总结语 */
    private String summary;
}

package com.example.aimaster.dto;

import lombok.Data;

/**
 * 职规/学习报告请求体：POST /api/report 时可选传 topic。
 */
@Data
public class ReportRequest {
    /** 报告主题或用户问题，例如「校招准备」「学习路线」「实习选择」 */
    private String topic;
}

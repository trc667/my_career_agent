package com.example.aimaster.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * AI 面试模拟：开始一次面试请求体。
 */
@Data
public class InterviewStartRequest {

    /** 面试岗位：后端 / 前端 / 算法 / 测试 / 运维 / 通用（空则随机混合出题） */
    @NotBlank(message = "请选择面试岗位")
    @Size(max = 16, message = "岗位不合法")
    private String position;
}

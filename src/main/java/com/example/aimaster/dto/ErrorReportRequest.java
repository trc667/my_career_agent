package com.example.aimaster.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 前端错误上报请求体（POST /api/monitor/report，公开接口）。
 * <p>
 * username / ip / userAgent 由后端从请求上下文解析，前端无需也不应传。
 */
@Data
public class ErrorReportRequest {

    /** 来源：frontend（默认）；后端异常由 GlobalExceptionHandler 直接入库，不走此接口 */
    @Size(max = 16, message = "source 长度不超过16")
    private String source;

    /** 级别：ERROR / WARN（默认 ERROR） */
    @Size(max = 16, message = "level 长度不超过16")
    private String level;

    /** 错误摘要（必填） */
    @NotBlank(message = "message 不能为空")
    @Size(max = 2000, message = "message 长度不超过2000")
    private String message;

    /** 错误详情/堆栈（可选） */
    private String stackTrace;

    /** 出错请求路径（可选） */
    @Size(max = 512, message = "uri 长度不超过512")
    private String uri;

    /** 出错请求方法（可选） */
    @Size(max = 16, message = "method 长度不超过16")
    private String method;
}

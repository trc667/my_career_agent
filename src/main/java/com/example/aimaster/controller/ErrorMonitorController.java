package com.example.aimaster.controller;

import com.example.aimaster.dto.ErrorReportRequest;
import com.example.aimaster.dto.Result;
import com.example.aimaster.service.ErrorLogService;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 错误监控上报接口（公开，限流防刷）。
 * <p>
 * 前端全局错误捕获（window.onerror / unhandledrejection / http 5xx）通过此接口上报，
 * 入库 error_log 供管理后台查看。SecurityConfig 已放行 /api/monitor/report。
 */
@RestController
@RequestMapping("/api/monitor")
public class ErrorMonitorController {

    private final ErrorLogService errorLogService;

    public ErrorMonitorController(ErrorLogService errorLogService) {
        this.errorLogService = errorLogService;
    }

    /** POST /api/monitor/report 前端错误上报（公开，60 次/分钟限流防刷） */
    @RateLimiter(name = "monitorLimiter")
    @PostMapping("/report")
    public Result<Void> report(@Valid @RequestBody ErrorReportRequest req, HttpServletRequest request) {
        errorLogService.report(req, request);
        return Result.ok("已记录", null);
    }
}

package com.example.aimaster.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.aimaster.dto.ErrorReportRequest;
import com.example.aimaster.entity.ErrorLog;
import com.example.aimaster.mapper.ErrorLogMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 错误日志服务：自建监控面板。
 * <ul>
 *   <li>report：前端上报（补全 IP/UA/username 后入库）</li>
 *   <li>reportBackend：后端全局异常自动入库（供 GlobalExceptionHandler 调用）</li>
 *   <li>list / clear：管理后台查询与清空</li>
 * </ul>
 * 所有入库均 try-catch，失败只打日志不影响主流程。
 */
@Service
public class ErrorLogService {

    private static final Logger log = LoggerFactory.getLogger(ErrorLogService.class);

    private final ErrorLogMapper errorLogMapper;

    public ErrorLogService(ErrorLogMapper errorLogMapper) {
        this.errorLogMapper = errorLogMapper;
    }

    /** 前端上报（source 默认 frontend） */
    public void report(ErrorReportRequest req, HttpServletRequest request) {
        try {
            ErrorLog row = ErrorLog.builder()
                    .level(normalize(req.getLevel(), "ERROR"))
                    .source(normalize(req.getSource(), "frontend"))
                    .message(truncate(req.getMessage(), 2000))
                    .stackTrace(truncate(req.getStackTrace(), 6000))
                    .uri(truncate(req.getUri(), 512))
                    .method(truncate(req.getMethod(), 16))
                    .username(currentUsername())
                    .userAgent(truncate(request != null ? request.getHeader("User-Agent") : "", 512))
                    .ip(truncate(resolveIp(request), 64))
                    .createTime(LocalDateTime.now())
                    .build();
            errorLogMapper.insert(row);
        } catch (Exception e) {
            log.warn("前端错误上报入库失败: {}", e.getMessage());
        }
    }

    /** 后端异常自动入库（GlobalExceptionHandler 调用，source 默认 backend） */
    public void reportBackend(String message, String stackTrace, String uri, String method) {
        try {
            ErrorLog row = ErrorLog.builder()
                    .level("ERROR")
                    .source("backend")
                    .message(truncate(message, 2000))
                    .stackTrace(truncate(stackTrace, 6000))
                    .uri(truncate(uri, 512))
                    .method(truncate(method, 16))
                    .username(currentUsername())
                    .createTime(LocalDateTime.now())
                    .build();
            errorLogMapper.insert(row);
        } catch (Exception e) {
            log.warn("后端异常入库失败: {}", e.getMessage());
        }
    }

    /** 管理后台：错误日志列表（倒序，可按来源/级别过滤） */
    public List<ErrorLog> list(String source, String level, int limit) {
        int safeLimit = Math.min(Math.max(limit <= 0 ? 100 : limit, 1), 500);
        LambdaQueryWrapper<ErrorLog> wrapper = new LambdaQueryWrapper<ErrorLog>()
                .eq(source != null && !source.isBlank(), ErrorLog::getSource, source)
                .eq(level != null && !level.isBlank(), ErrorLog::getLevel, level)
                .orderByDesc(ErrorLog::getCreateTime)
                .last("LIMIT " + safeLimit);
        return errorLogMapper.selectList(wrapper);
    }

    /** 管理后台：清空错误日志 */
    public void clear() {
        errorLogMapper.delete(new LambdaQueryWrapper<>());
    }

    private static String normalize(String value, String fallback) {
        if (value == null || value.isBlank()) return fallback;
        return value.trim();
    }

    private static String truncate(String value, int max) {
        if (value == null) return "";
        return value.length() > max ? value.substring(0, max) : value;
    }

    /** 从 SecurityContext 解析当前用户名（未登录返回空） */
    private static String currentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) return "";
        String name = auth.getName();
        return ("anonymousUser".equals(name) || "anonymous".equals(name)) ? "" : name;
    }

    /** 解析客户端 IP（兼容反向代理 X-Forwarded-For） */
    private static String resolveIp(HttpServletRequest request) {
        if (request == null) return "";
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            int comma = forwarded.indexOf(',');
            return comma > 0 ? forwarded.substring(0, comma).trim() : forwarded.trim();
        }
        return request.getRemoteAddr();
    }
}

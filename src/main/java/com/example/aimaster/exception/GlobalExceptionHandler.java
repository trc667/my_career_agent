package com.example.aimaster.exception;

import com.example.aimaster.dto.Result;
import com.example.aimaster.service.ErrorLogService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import io.github.resilience4j.ratelimiter.RequestNotPermitted;

import org.apache.catalina.connector.ClientAbortException;

import java.util.stream.Collectors;

/**
 * 全局异常处理，将各类异常统一封装为 Result 返回。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private final ErrorLogService errorLogService;

    public GlobalExceptionHandler(ErrorLogService errorLogService) {
        this.errorLogService = errorLogService;
    }

    /** 参数校验失败（@Valid）：如消息为空、超长等 */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<?> handleValidation(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        log.warn("参数校验失败: {}", msg);
        return Result.fail(400, msg.isEmpty() ? "参数校验失败" : msg);
    }

    /** 请求体解析失败（如 JSON 格式错误） */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<?> handleMessageNotReadable(HttpMessageNotReadableException e) {
        log.warn("请求体解析失败: {}", e.getMessage());
        return Result.fail(400, "请求体格式错误，请检查 JSON 格式");
    }

    /** 缺少必填参数 */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<?> handleMissingParam(MissingServletRequestParameterException e) {
        log.warn("缺少参数: {}", e.getParameterName());
        return Result.fail(400, "缺少必填参数: " + e.getParameterName());
    }

    /** 业务异常（可选：若项目有 BusinessException） */
    @ExceptionHandler(BusinessException.class)
    @ResponseStatus(HttpStatus.OK)
    public Result<?> handleBusiness(BusinessException e) {
        log.warn("业务异常: {}", e.getMessage());
        return Result.fail(e.getCode() > 0 ? e.getCode() : 400, e.getMessage());
    }

    /** 无静态资源（如 /favicon.ico）：不打 ERROR 堆栈，避免刷屏 */
    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Result<?> handleNoResource(NoResourceFoundException e) {
        log.debug("静态资源未找到: {}", e.getResourcePath());
        return Result.fail(404, "资源不存在");
    }

    /**
     * SSE/流式响应时客户端关闭标签页、刷新或代理断开连接，属正常情况。
     * 必须 void 返回，勿写 JSON，否则会与 text/event-stream 冲突（HttpMessageNotWritableException）。
     */
    @ExceptionHandler({ClientAbortException.class, AsyncRequestNotUsableException.class})
    public void handleClientDisconnected(Exception e) {
        log.debug("客户端已断开连接: {}", e.getClass().getSimpleName());
    }

    /** 其他未捕获异常：异常详情只落日志与 error_log，不下发客户端（避免泄露内部类名/错误信息） */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<?> handleOther(Exception e, HttpServletRequest request) {
        log.error("未捕获异常", e);
        // 自建监控：500 级错误自动入库（入库失败不影响响应）
        try {
            String uri = request != null ? request.getRequestURI() : "";
            String method = request != null ? request.getMethod() : "";
            String msg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            if (msg.length() > 500) msg = msg.substring(0, 497) + "...";
            errorLogService.reportBackend(msg, stackTraceOf(e), uri, method);
        } catch (Exception ignored) {
            // 监控入库失败不阻塞主流程
        }
        return Result.fail(500, "服务异常，请稍后重试");
    }

    /** 截取堆栈前若干行（防止超长入库） */
    private String stackTraceOf(Throwable e) {
        StackTraceElement[] trace = e.getStackTrace();
        if (trace == null || trace.length == 0) return e.toString();
        StringBuilder sb = new StringBuilder(e.toString()).append("\n");
        int limit = Math.min(trace.length, 20);
        for (int i = 0; i < limit; i++) {
            sb.append("\tat ").append(trace[i]).append("\n");
        }
        return sb.toString();
    }
    /** 限流：请求过于频繁 */
    @ExceptionHandler(RequestNotPermitted.class)
    @ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)//429
    public Result<?>handleRatelimit(RequestNotPermitted e){
        log.warn("限流触发：{}",e.getMessage());
        return Result.fail(429,"请求过于频繁");
    }
}


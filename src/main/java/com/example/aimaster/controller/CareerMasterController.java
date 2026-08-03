package com.example.aimaster.controller;

import com.example.aimaster.dto.ChatRequest;
import com.example.aimaster.dto.ChatResponse;
import com.example.aimaster.dto.ChatStreamSession;
import com.example.aimaster.dto.CareerReport;
import com.example.aimaster.filter.SensitiveWordFilter;
import com.example.aimaster.dto.ReportRequest;
import com.example.aimaster.dto.Result;
import com.example.aimaster.memory.ConversationMemoryStore;
import com.example.aimaster.service.CareerMasterService;
import jakarta.validation.Valid;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.security.concurrent.DelegatingSecurityContextRunnable;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * 计算机学生职规大师智能体 REST 接口（控制层），统一返回 Result。
 */
@RestController
@RequestMapping("/api")
public class CareerMasterController {

    /** 客户端关页/刷新导致连接断开，无需打 ERROR 或 completeWithError */
    private static boolean isClientDisconnected(Throwable e) {
        for (Throwable t = e; t != null; t = t.getCause()) {
            String n = t.getClass().getName();
            if (n.contains("ClientAbort") || n.contains("AsyncRequestNotUsable")) {
                return true;
            }
        }
        return false;
    }

    private final CareerMasterService careerMasterService;
    private final ConversationMemoryStore memoryStore;
    private final SensitiveWordFilter sensitiveWordFilter;

    public CareerMasterController(CareerMasterService careerMasterService,
                                ConversationMemoryStore memoryStore,
                                @Autowired(required = false) SensitiveWordFilter sensitiveWordFilter) {
        this.careerMasterService = careerMasterService;
        this.memoryStore = memoryStore;
        this.sensitiveWordFilter = sensitiveWordFilter;
    }

@RateLimiter(name = "chatLimiter")
    @PostMapping(value = "/chat", produces = "application/json; charset=UTF-8")
    public Result<ChatResponse> chat(@Valid @RequestBody ChatRequest request) {
        ChatResponse response = careerMasterService.chatWithRag(request.getConversationId(), request.getMessage());
        return Result.ok(response);
    }
@RateLimiter(name="chatLimiter")
    @GetMapping(value = "/chat", produces = "application/json; charset=UTF-8")
    public Result<ChatResponse> chatGet(@RequestParam(required = false) String message,
                                        @RequestParam(required = false) String conversationId) {
        if (message == null || message.isBlank()) {
            return Result.fail(400, "请加上参数: message=你的问题");
        }
        ChatResponse response = careerMasterService.chatWithRag(conversationId, message.trim());
        return Result.ok(response);
    }
    @RateLimiter(name="chatLimiter")
    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chatStream(@Valid @RequestBody ChatRequest request) {
        SseEmitter emitter = new SseEmitter(180_000L);
        String userMessage = request.getMessage();
        String conversationId = request.getConversationId();
        ChatStreamSession streamSession = careerMasterService.chatWithRagStream(conversationId, userMessage);
        StringBuilder fullReply = new StringBuilder();
        SecurityContext ctx = SecurityContextHolder.getContext();

        streamSession.flux().subscribe(
                chunk -> {
                    SecurityContextHolder.setContext(ctx);
                    String text = (chunk instanceof CharSequence) ? chunk.toString() : String.valueOf(chunk);
                    fullReply.append(text);
                    try {
                        emitter.send(SseEmitter.event().data(text));
                    } catch (IOException e) {
                        if (isClientDisconnected(e)) {
                            try {
                                emitter.complete();
                            } catch (Exception ignored) {
                            }
                        } else {
                            emitter.completeWithError(e);
                        }
                    }
                },
                error -> {
                    SecurityContextHolder.setContext(ctx);
                    if (isClientDisconnected(error)) {
                        try {
                            emitter.complete();
                        } catch (Exception ignored) {
                        }
                    } else {
                        emitter.completeWithError(error);
                    }
                },
                () -> {
                    SecurityContextHolder.setContext(ctx);
                    try {
                        String userToStore = sensitiveWordFilter != null && sensitiveWordFilter.isLoaded()
                                ? sensitiveWordFilter.filter(userMessage) : userMessage;
                        String replyToStore = sensitiveWordFilter != null && sensitiveWordFilter.isLoaded()
                                ? sensitiveWordFilter.filter(fullReply.toString()) : fullReply.toString();
                        memoryStore.add(streamSession.conversationId(), new UserMessage(userToStore));
                        memoryStore.add(streamSession.conversationId(), new AssistantMessage(replyToStore));
                        emitter.send(SseEmitter.event().name("conversationId").data(streamSession.conversationId()));
                        Runnable completeTask = () -> { try { emitter.complete(); } catch (Exception ignored) {} };
                        CompletableFuture.delayedExecutor(150, TimeUnit.MILLISECONDS)
                                .execute(new DelegatingSecurityContextRunnable(completeTask, ctx));
                    } catch (IOException e) {
                        if (isClientDisconnected(e)) {
                            try {
                                emitter.complete();
                            } catch (Exception ignored) {
                            }
                        } else {
                            emitter.completeWithError(e);
                        }
                    }
                }
        );
        return emitter;
    }
    @RateLimiter(name="chatLimiter")
    @PostMapping(value = "/chat/react", produces = "application/json; charset=UTF-8")
    public Result<ChatResponse> chatReact(@Valid @RequestBody ChatRequest request,
                                          @RequestParam(required = false, defaultValue = "8") int maxSteps) {
        ChatResponse response = careerMasterService.chatWithReAct(
                request.getConversationId(),
                request.getMessage(),
                maxSteps
        );
        return Result.ok(response);
    }
    @RateLimiter(name="chatLimiter")
    @PostMapping(value = "/chat/react/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chatReactStream(@Valid @RequestBody ChatRequest request,
                                      @RequestParam(required = false, defaultValue = "8") int maxSteps) {
        SseEmitter emitter = new SseEmitter(300_000L);
        String userMessage = request.getMessage();
        String conversationId = request.getConversationId();
        try {
            emitter.send(SseEmitter.event().data("{\"type\":\"start\",\"content\":\"连接成功\"}"));
        } catch (IOException e) {
            emitter.complete();
            return emitter;
        }

        SecurityContext ctx = SecurityContextHolder.getContext();
        Runnable task = () -> {
            try {
                if (userMessage == null || userMessage.isBlank()) {
                    emitter.send(SseEmitter.event().data("{\"type\":\"error\",\"content\":\"消息不能为空\"}"));
                    return;
                }
                careerMasterService.chatWithReActStream(conversationId, userMessage, maxSteps, chunk -> {
                    try {
                        String text = (chunk instanceof String) ? (String) chunk : String.valueOf(chunk);
                        emitter.send(SseEmitter.event().data(text));
                    } catch (IOException e) {
                        if (isClientDisconnected(e)) {
                            return;
                        }
                        throw new RuntimeException("SSE 发送失败", e);
                    }
                });
            } catch (Exception e) {
                try {
                    String msg = e.getMessage() != null ? e.getMessage() : "执行异常";
                    if (msg.length() > 200) {
                        msg = msg.substring(0, 200);
                    }
                    String escaped = msg.replace("\\", "\\\\").replace("\"", "'");
                    emitter.send(SseEmitter.event().data("{\"type\":\"error\",\"content\":\"" + escaped + "\"}"));
                } catch (Exception ignored) {
                }
            } finally {
                // 短暂延迟再 complete，避免客户端 ERR_INCOMPLETE_CHUNKED_ENCODING
                CompletableFuture.delayedExecutor(100, TimeUnit.MILLISECONDS)
                        .execute(new DelegatingSecurityContextRunnable(() -> {
                            try {
                                emitter.complete();
                            } catch (IllegalStateException ignored) {
                            }
                        }, ctx));
            }
        };
        CompletableFuture.runAsync(new DelegatingSecurityContextRunnable(task, ctx));

        emitter.onTimeout(() -> {
            SecurityContextHolder.setContext(ctx);
            try {
                try {
                    emitter.send(SseEmitter.event().data("{\"type\":\"error\",\"content\":\"请求超时\"}"));
                } catch (Exception ignored) {
                }
                emitter.complete();
            } finally {
                SecurityContextHolder.clearContext();
            }
        });
        return emitter;
    }
    @RateLimiter(name="chatLimiter")
    @PostMapping(value = "/report", produces = "application/json; charset=UTF-8")
    public Result<CareerReport> report(@RequestBody(required = false) ReportRequest body) {
        String topic = body != null && body.getTopic() != null ? body.getTopic() : "";
        CareerReport report = careerMasterService.generateReport(topic);
        return Result.ok(report);
    }
    @RateLimiter(name="chatLimiter")
    @GetMapping(value = "/report", produces = "application/json; charset=UTF-8")
    public Result<CareerReport> reportGet(@RequestParam(required = false) String topic) {
        CareerReport report = careerMasterService.generateReport(topic != null ? topic : "");
        return Result.ok(report);
    }

    @GetMapping(value = "/health", produces = "application/json; charset=UTF-8")
    public Result<String> health() {
        return Result.ok("ok");
    }
}

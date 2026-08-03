package com.example.aimaster.tool;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;

/**
 * 包装 ToolCallback，在每次调用时输出日志，用于追踪 DashScope 内部执行的工具。
 */
public class LoggingToolCallback implements ToolCallback {

    private static final Logger log = LoggerFactory.getLogger(LoggingToolCallback.class);

    private final ToolCallback delegate;

    public LoggingToolCallback(ToolCallback delegate) {
        this.delegate = delegate;
    }

    @Override
    public org.springframework.ai.tool.definition.ToolDefinition getToolDefinition() {
        return delegate.getToolDefinition();
    }

    @Override
    public String call(String toolInput) {
        String name = getToolDefinition() != null ? getToolDefinition().name() : "unknown";
        String argsPreview = toolInput != null && toolInput.length() > 80
                ? toolInput.substring(0, 80) + "..."
                : (toolInput != null ? toolInput : "");
        log.info("  [工具调用] {}  args={}", name, argsPreview);
        long start = System.currentTimeMillis();
        try {
            String result = delegate.call(toolInput);
            log.info("  [工具返回] {}  耗时 {}ms", name, System.currentTimeMillis() - start);
            return result;
        } catch (Exception e) {
            log.warn("  [工具异常] {}  {}", name, e.getMessage());
            throw e;
        }
    }

    @Override
    public String call(String toolInput, ToolContext toolContext) {
        String name = getToolDefinition() != null ? getToolDefinition().name() : "unknown";
        String argsPreview = toolInput != null && toolInput.length() > 80
                ? toolInput.substring(0, 80) + "..."
                : (toolInput != null ? toolInput : "");
        log.info("  [工具调用] {}  args={}", name, argsPreview);
        long start = System.currentTimeMillis();
        try {
            String result = delegate.call(toolInput, toolContext);
            log.info("  [工具返回] {}  耗时 {}ms", name, System.currentTimeMillis() - start);
            return result;
        } catch (Exception e) {
            log.warn("  [工具异常] {}  {}", name, e.getMessage());
            throw e;
        }
    }
}

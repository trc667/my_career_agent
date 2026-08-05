package com.example.aimaster.service.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;

import com.example.aimaster.dto.ChatResponse;
import com.example.aimaster.dto.ChatStreamSession;
import com.example.aimaster.dto.CareerReport;
import com.example.aimaster.dto.ReActStep;
import com.example.aimaster.filter.SensitiveWordFilter;
import com.example.aimaster.memory.ConversationMemoryStore;
import com.example.aimaster.rag.RagDocumentLoader;
import com.example.aimaster.service.CareerMasterService;
import com.example.aimaster.tool.FileTool;
import com.example.aimaster.tool.LoggingToolCallback;
import com.example.aimaster.rag.HybridRetriever;
import com.example.aimaster.tool.NoteTool;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.model.StreamingChatModel;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;

import reactor.core.publisher.Flux;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 计算机学生职规大师智能体服务实现（业务层）。
 */
@Service
public class CareerMasterServiceImpl implements CareerMasterService {

    private static final Logger log = LoggerFactory.getLogger(CareerMasterServiceImpl.class);

    /** 高德地图工具调用规范：附近图书馆/自习室/咖啡馆时先用 geo/place_search 再 around_search。 */
    private static final String AMAP_TOOL_GUIDELINES =
            "【高德地图工具调用规范】\n"
            + "当用户问「附近图书馆」「哪里可以自习」「附近咖啡馆」时，先用 geo 或 place_search 根据地点名称（如「深圳翠竹地铁站」）获取真实经纬度，再将返回的 location 传给 around_search。\n"
            + "禁止使用估计或猜测的坐标（如 114.123456），否则将返回空 POI 数据。";

    private static final String REPORT_SYSTEM_PROMPT =
            "你是一位计算机学生职业规划与学习顾问。请根据用户给出的主题，生成一份简洁的「职规/学习建议报告」。\n"
            + "只输出一个合法 JSON，格式如下：\n"
            + "{\"title\":\"报告标题\",\"suggestions\":[\"建议1\",\"建议2\",\"建议3\"],\"summary\":\"总结语\"}\n"
            + "不要输出其他文字，不要用 ```json 包裹。";

    private final ChatModel chatModel;
    private final StreamingChatModel streamingChatModel;
    private final String systemPrompt;
    private final ConversationMemoryStore memoryStore;
    private final RagDocumentLoader ragDocumentLoader;
    private final HybridRetriever hybridRetriever;
    private final SyncMcpToolCallbackProvider mcpToolCallbackProvider;
    private final NoteTool noteTool;
    private final FileTool fileTool;
    private final ObjectMapper objectMapper;
    private final SensitiveWordFilter sensitiveWordFilter;
    private final boolean reactStreamToolsEnabled;
    private final boolean reactStreamMcpEnabled;
    private final int reactStreamTypingDelayMs;
    private final int reactStepTimeoutMs;

    public CareerMasterServiceImpl(
            ChatModel chatModel,
            @Autowired(required = false) StreamingChatModel streamingChatModel,
            @Qualifier("careerMasterSystemPrompt") String systemPrompt,
            ConversationMemoryStore memoryStore,
            RagDocumentLoader ragDocumentLoader,
            HybridRetriever hybridRetriever,
            @Autowired(required = false) SyncMcpToolCallbackProvider mcpToolCallbackProvider,
            @Autowired(required = false) NoteTool noteTool,
            @Autowired(required = false) FileTool fileTool,
            ObjectMapper objectMapper,
            @Autowired(required = false) SensitiveWordFilter sensitiveWordFilter,
            @Value("${app.react-stream.tools-enabled:true}") boolean reactStreamToolsEnabled,
            @Value("${app.react-stream.mcp-enabled:false}") boolean reactStreamMcpEnabled,
            @Value("${app.react-stream.typing-delay-ms:40}") int reactStreamTypingDelayMs,
            @Value("${app.react-stream.step-timeout-ms:30000}") int reactStepTimeoutMs) {
        this.chatModel = chatModel;
        this.streamingChatModel = streamingChatModel;
        this.systemPrompt = systemPrompt;
        this.memoryStore = memoryStore;
        this.ragDocumentLoader = ragDocumentLoader;
        this.hybridRetriever = hybridRetriever;
        this.mcpToolCallbackProvider = mcpToolCallbackProvider;
        this.noteTool = noteTool;
        this.fileTool = fileTool;
        this.objectMapper = objectMapper;
        this.sensitiveWordFilter = sensitiveWordFilter;
        this.reactStreamToolsEnabled = reactStreamToolsEnabled;
        this.reactStreamMcpEnabled = reactStreamMcpEnabled;
        this.reactStreamTypingDelayMs = reactStreamTypingDelayMs;
        this.reactStepTimeoutMs = Math.max(5_000, reactStepTimeoutMs);
    }

    private static final String SENSITIVE_WORD_HINT =
            "您输入的内容包含敏感词，已自动过滤。请重新输入有效内容。";

    private String filterText(String text) {
        if (text == null) return null;
        return sensitiveWordFilter != null && sensitiveWordFilter.isLoaded() ? sensitiveWordFilter.filter(text) : text;
    }

    /** 判断过滤后是否无有效内容（空、空白或仅剩替换符），若是则返回提示文案。 */
    private String checkFilteredEmpty(String original, String filtered) {
        if (original == null || original.isBlank()) {
            return null;
        }
        if (filtered == null || filtered.isBlank()) {
            return SENSITIVE_WORD_HINT;
        }
        if (sensitiveWordFilter != null && sensitiveWordFilter.isLoaded()
                && sensitiveWordFilter.isFullyFiltered(filtered)) {
            return SENSITIVE_WORD_HINT;
        }
        return null;
    }

    @PostConstruct
    public void logToolCount() {
        ToolCallback[] all = getMergedToolCallbacks();
        if (all.length == 0) {
            log.warn("---------- 工具加载为 0，请确认：1) spring.profiles.active=dev  2) application-dev.yml 含 MCP stdio 配置  3) Node.js/npx 可用 ----------");
        } else {
            log.info("---------- 已加载 {} 个工具 ----------", all.length);
            for (ToolCallback cb : all) {
                if (cb.getToolDefinition() != null) {
                    String name = cb.getToolDefinition().name();
                    String desc = cb.getToolDefinition().description();
                    String descShort = (desc != null && !desc.isBlank()) ? (desc.length() > 60 ? desc.substring(0, 60) + "..." : desc) : "";
                    log.info("  - {} {}", name != null ? name : "(unnamed)", descShort);
                }
            }
        }
    }

    @Override
    public ChatResponse chat(String userMessage) {
        String input = filterText(userMessage);
        String hint = checkFilteredEmpty(userMessage, input);
        if (hint != null) {
            return ChatResponse.builder().reply(hint).build();
        }
        String sys = systemPrompt.replace("{context}", "");
        Prompt prompt = new Prompt(List.of(
                new SystemMessage(sys),
                new UserMessage(input)
        ));
        org.springframework.ai.chat.model.ChatResponse response = chatModel.call(prompt);
        String reply = filterText(extractReplyText(response));
        Integer usage = response.getMetadata() != null && response.getMetadata().getUsage() != null
                ? response.getMetadata().getUsage().getTotalTokens()
                : null;
        log.info("---------- 用户提问 ----------");
        log.info("{}", userMessage);
        log.info("---------- AI 职规大师 回复 ----------");
        log.info("{}", reply);
        if (usage != null) {
            log.info("---------- 本次消耗 token: {} ----------", usage);
        }
        String safeReply = reply != null ? reply : "";
        return ChatResponse.builder().reply(safeReply).usageTokens(usage).build();
    }

    @Override
    public ChatResponse chat(String conversationId, String userMessage) {
        String id = conversationId != null && !conversationId.isBlank()
                ? conversationId.trim()
                : UUID.randomUUID().toString();
        String input = filterText(userMessage);
        String hint = checkFilteredEmpty(userMessage, input);
        if (hint != null) {
            return ChatResponse.builder().conversationId(id).reply(hint).build();
        }

        List<Message> history = memoryStore.getMessages(id);
        List<Message> msglist = new ArrayList<>();
        msglist.add(new SystemMessage(systemPrompt.replace("{context}", "")));
        msglist.addAll(history);
        msglist.add(new UserMessage(input));

        Prompt prompt = new Prompt(msglist);
        org.springframework.ai.chat.model.ChatResponse response = chatModel.call(prompt);

        String reply = filterText(extractReplyText(response));
        Integer usage = response.getMetadata() != null && response.getMetadata().getUsage() != null
                ? response.getMetadata().getUsage().getTotalTokens()
                : null;

        memoryStore.add(id, new UserMessage(input));
        memoryStore.add(id, new AssistantMessage(reply != null ? reply : ""));

        return ChatResponse.builder()
                .conversationId(id).usageTokens(usage).reply(reply != null ? reply : "")
                .build();
    }

    private String extractReplyText(org.springframework.ai.chat.model.ChatResponse response) {
        if (response == null) return "";
        Generation gen = response.getResult();
        if (gen == null && response.getResults() != null && !response.getResults().isEmpty()) {
            gen = response.getResults().get(0);
        }
        if (gen == null || gen.getOutput() == null) {
            log.warn("模型返回无有效结果，请检查 API Key 与网络");
            return "";
        }
        String text = gen.getOutput().getText();
        if (text == null || text.isEmpty()) {
            log.warn("模型返回内容为空，请检查 application-dev.yml 中的 API Key 与网络");
            return "";
        }
        return text;
    }

    @Override
    public ChatResponse chatWithRag(String conversationId, String userMessage) {
        String id = conversationId != null && !conversationId.isBlank()
                ? conversationId.trim()
                : UUID.randomUUID().toString();
        String input = filterText(userMessage);
        String hint = checkFilteredEmpty(userMessage, input);
        if (hint != null) {
            return ChatResponse.builder().conversationId(id).reply(hint).build();
        }

        String context = retrieveRagContext(input);
        String systemWithContext = systemPrompt.replace("{context}", context != null ? context : "");
        List<Message> msgList = new ArrayList<>();
        msgList.add(new SystemMessage(systemWithContext));
        msgList.addAll(memoryStore.getMessages(id));
        msgList.add(new UserMessage(input));
        Prompt prompt = new Prompt(msgList);
        org.springframework.ai.chat.model.ChatResponse call = chatModel.call(prompt);
        String reply = filterText(extractReplyText(call));
        Integer usage = call.getMetadata() != null && call.getMetadata().getUsage() != null
                ? call.getMetadata().getUsage().getTotalTokens()
                : null;
        memoryStore.add(id, new UserMessage(input));
        memoryStore.add(id, new AssistantMessage(reply != null ? reply : ""));

        return ChatResponse.builder().conversationId(id).usageTokens(usage).reply(reply != null ? reply : "").build();
    }

    /**
     * 混合检索：向量 + BM25 → RRF 融合 → qwen3-rerank 精排，拼成上下文文本。
     * 异常时降级为原纯向量检索（top4），保证主链路不中断。
     */
    private String retrieveRagContext(String query) {
        try {
            List<Document> docs = hybridRetriever.retrieve(query, 5);
            return docs.stream()
                    .map(Document::getText)
                    .filter(s -> s != null && !s.isBlank())
                    .collect(Collectors.joining("\n\n"));
        } catch (Exception e) {
            log.warn("混合检索失败，退化纯向量检索：{}", e.getMessage());
            return ragDocumentLoader.retrieveContext(query, 4);
        }
    }

    @Override
    public ChatStreamSession chatWithRagStream(String conversationId, String userMessage) {
        String id = conversationId != null && !conversationId.isBlank() ? conversationId.trim() : UUID.randomUUID().toString();
        String input = filterText(userMessage);
        String hint = checkFilteredEmpty(userMessage, input);
        if (hint != null) {
            return new ChatStreamSession(id, Flux.just(hint));
        }
        String context = retrieveRagContext(input);
        String systemWithContext = systemPrompt.replace("{context}", context != null ? context : "");
        List<Message> msgList = new ArrayList<>();
        msgList.add(new SystemMessage(systemWithContext));
        msgList.addAll(memoryStore.getMessages(id));
        msgList.add(new UserMessage(input));
        Flux<String> flux = streamingChatModel != null
                ? streamingChatModel.stream(new Prompt(msgList))
                        .mapNotNull(r -> r != null && r.getResult() != null && r.getResult().getOutput() != null ? r.getResult().getOutput().getText() : null)
                        .filter(s -> s != null && !s.isEmpty())
                        .map(this::filterText)
                : Flux.just(filterText(extractReplyText(chatModel.call(new Prompt(msgList)))));
        return new ChatStreamSession(id, flux);
    }

    @Override
    public CareerReport generateReport(String topic) {
        String inputTopic = filterText((topic != null && !topic.isBlank()) ? topic : "计算机专业校招准备与学习路线");
        if (inputTopic == null) {
            inputTopic = "计算机专业校招准备与学习路线";
        }
        Prompt prompt = new Prompt(List.of(new SystemMessage(REPORT_SYSTEM_PROMPT), new UserMessage("请为「" + inputTopic + "」生成职规/学习建议报告。")));
        String raw = extractReplyText(chatModel.call(prompt));
        if (raw == null || raw.isBlank()) return new CareerReport("报告", List.of("暂无建议"), "请稍后重试");
        String json = raw;
        if (raw.contains("```")) {
            int s = raw.indexOf("```");
            int e = raw.indexOf("```", s + 3);
            if (e > s) {
                json = raw.substring(s + 3, e).replace("json", "").trim();
            }
        }
        try {
            CareerReport report = objectMapper.readValue(json, CareerReport.class);
            report = new CareerReport(
                    filterText(report.getTitle()),
                    report.getSuggestions() != null ? report.getSuggestions().stream().map(this::filterText).toList() : List.of(),
                    filterText(report.getSummary())
            );
            return report;
        } catch (Exception ex) {
            log.warn("报告 JSON 解析失败: {}", ex.getMessage());
            return new CareerReport("报告", List.of(raw.length() > 200 ? raw.substring(0, 200) + "..." : raw), "解析异常");
        }
    }

    private ToolCallback[] getMergedToolCallbacks() {
        List<ToolCallback> list = new ArrayList<>();
        if (reactStreamMcpEnabled && mcpToolCallbackProvider != null) {
            ToolCallback[] mcp = mcpToolCallbackProvider.getToolCallbacks();
            if (mcp != null) {
                for (ToolCallback c : mcp) {
                    list.add(new LoggingToolCallback(c));
                }
            }
        }
        if (noteTool != null) {
            for (ToolCallback c : ToolCallbacks.from(noteTool)) list.add(new LoggingToolCallback(c));
        }
        if (fileTool != null) {
            for (ToolCallback c : ToolCallbacks.from(fileTool)) list.add(new LoggingToolCallback(c));
        }
        return list.toArray(new ToolCallback[0]);
    }

    private ChatOptions buildToolCallOptionsForReAct() {
        ToolCallback[] all = getMergedToolCallbacks();
        if (all.length == 0) return null;
        return ToolCallingChatOptions.builder().toolCallbacks(all).build();
    }

    private ChatResponse executeToolLoop(String id, List<Message> msgList, ChatOptions toolOptions, String userMessage) {
        int step = 0;
        int maxSteps = 20;
        org.springframework.ai.chat.model.ChatResponse response = null;
        List<ReActStep> steps = new ArrayList<>();
        Map<String, ToolCallback> byName = new java.util.HashMap<>();
        for (ToolCallback cb : getMergedToolCallbacks()) {
            if (cb.getToolDefinition() != null && cb.getToolDefinition().name() != null)
                byName.put(cb.getToolDefinition().name(), cb);
        }
        while (step < maxSteps) {
            step++;
            log.info("ReAct 步骤 {}/{}", step, maxSteps);
            Prompt prompt = new Prompt(msgList, toolOptions);
            response = callModelWithTimeout(prompt);
            Generation gen = response.getResult();
            if (gen == null && response.getResults() != null && !response.getResults().isEmpty())
                gen = response.getResults().get(0);
            if (gen == null || gen.getOutput() == null) {
                log.info("ReAct 完成: 共 {} 步，模型无有效输出", step);
                return ChatResponse.builder().conversationId(id).reply("抱歉，本轮没有获得有效回复。").steps(List.of()).build();
            }
            Message out = gen.getOutput();
            if (out instanceof AssistantMessage am && am.hasToolCalls()) {
                List<AssistantMessage.ToolCall> tcs = am.getToolCalls();
                if (tcs == null || tcs.isEmpty()) {
                    String reply = filterText(am.getText() != null ? am.getText() : "");
                    memoryStore.add(id, new UserMessage(userMessage));
                    memoryStore.add(id, new AssistantMessage(reply != null ? reply : ""));
                    log.info("ReAct 完成: 共 {} 步，无工具调用", step);
                    return ChatResponse.builder().conversationId(id).reply(reply).usageTokens(
                            response.getMetadata() != null && response.getMetadata().getUsage() != null
                                    ? response.getMetadata().getUsage().getTotalTokens() : null).steps(steps).build();
                }
                if (am.getText() != null && !am.getText().isBlank())
                    steps.add(new ReActStep("thought", am.getText(), null, null));
                List<ToolResponseMessage.ToolResponse> trs = new ArrayList<>();
                for (AssistantMessage.ToolCall tc : tcs) {
                    log.info("  调用工具: {} args={}", tc.name(), tc.arguments() != null ? tc.arguments() : "");
                    steps.add(new ReActStep("tool_call", "调用 " + tc.name(), tc.name(), tc.arguments()));
                    ToolCallback cb = byName.get(tc.name());
                    String res = cb != null ? safeCallTool(cb, tc.arguments() != null ? tc.arguments() : "", tc.name()) : "未找到工具";
                    steps.add(new ReActStep("tool_result", res != null ? res : "", null, null));
                    trs.add(new ToolResponseMessage.ToolResponse(tc.id(), tc.name(), res != null ? res : ""));
                }
                msgList.add(am);
                msgList.add(new ToolResponseMessage(trs));
            } else {
                String reply = filterText(extractReplyText(response));
                memoryStore.add(id, new UserMessage(userMessage));
                memoryStore.add(id, new AssistantMessage(reply != null ? reply : ""));
                java.util.List<String> toolNames = steps.stream()
                        .filter(s -> s.getType() != null && "tool_call".equals(s.getType()))
                        .map(s -> s.getContent() != null ? s.getContent().replaceFirst("调用 ", "") : "")
                        .filter(n -> !n.isEmpty())
                        .distinct()
                        .toList();
                log.info("ReAct 完成: 共 {} 步，调用工具: {}", step, toolNames.isEmpty() ? "无" : toolNames);
                return ChatResponse.builder().conversationId(id).reply(reply != null ? reply : "")
                        .usageTokens(response.getMetadata() != null && response.getMetadata().getUsage() != null
                                ? response.getMetadata().getUsage().getTotalTokens() : null).steps(steps).build();
            }
        }
        String lastReply = response != null ? filterText(extractReplyText(response)) : "";
        if (lastReply == null || lastReply.isBlank()) {
            lastReply = "已达到最大规划步数，请简化问题。";
        }
        memoryStore.add(id, new UserMessage(userMessage));
        memoryStore.add(id, new AssistantMessage(lastReply));
        java.util.List<String> toolNames = steps.stream()
                .filter(s -> s.getType() != null && "tool_call".equals(s.getType()))
                .map(s -> s.getToolName() != null ? s.getToolName() : (s.getContent() != null ? s.getContent().replaceFirst("调用 ", "") : ""))
                .filter(n -> !n.isEmpty())
                .distinct()
                .toList();
        log.info("ReAct 完成: 共 {} 步（达最大步数），调用工具: {}", step, toolNames.isEmpty() ? "无" : toolNames);
        return ChatResponse.builder().conversationId(id).reply(lastReply).steps(steps).build();
    }

    @Override
    public ChatResponse chatWithReAct(String conversationId, String userMessage, int maxSteps) {
        String id = conversationId != null && !conversationId.isBlank() ? conversationId.trim() : UUID.randomUUID().toString();
        String filtered = filterText(userMessage);
        String input = filtered != null ? filtered : (userMessage != null ? userMessage : "");
        String hint = checkFilteredEmpty(userMessage, input);
        if (hint != null) {
            return ChatResponse.builder().conversationId(id).reply(hint).steps(List.of()).build();
        }
        if (maxSteps <= 0) {
            String msg = "maxSteps 必须大于 0。";
            memoryStore.add(id, new UserMessage(input));
            memoryStore.add(id, new AssistantMessage(msg));
            return ChatResponse.builder().conversationId(id).reply(msg).steps(List.of()).build();
        }
        List<Message> msgList = new ArrayList<>();
        msgList.add(new SystemMessage(systemPrompt.replace("{context}", "") + "\n\n" + AMAP_TOOL_GUIDELINES));
        msgList.addAll(memoryStore.getMessages(id));
        msgList.add(new UserMessage(input));
        ChatOptions toolOptions = buildToolCallOptionsForReAct();
        if (toolOptions == null) {
            org.springframework.ai.chat.model.ChatResponse resp = callModelWithTimeout(new Prompt(msgList));
            String reply = filterText(extractReplyText(resp));
            memoryStore.add(id, new UserMessage(input));
            memoryStore.add(id, new AssistantMessage(reply != null ? reply : ""));
            return ChatResponse.builder().conversationId(id).reply(reply != null ? reply : "").steps(List.of()).build();
        }
        log.info("---------- ReAct 请求开始: message=[{}], maxSteps={} ----------", input, maxSteps);
        return executeToolLoop(id, msgList, toolOptions, input);
    }

    private ToolCallback[] getMergedToolCallbacksForReActStream() {
        return getMergedToolCallbacks();
    }

    private ChatOptions buildToolCallOptionsForReActStream() {
        ToolCallback[] all = getMergedToolCallbacksForReActStream();
        if (all.length == 0) return null;
        return ToolCallingChatOptions.builder().toolCallbacks(all).build();
    }

    private void sendReplyInChunks(String fullReply, java.util.function.Consumer<String> stepConsumer) {
        if (fullReply == null) {
            fullReply = "";
        }
        int chunkSize = 2;
        for (int i = 0; i < fullReply.length(); i += chunkSize) {
            if (i > 0 && reactStreamTypingDelayMs > 0) {
                try {
                    Thread.sleep(reactStreamTypingDelayMs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
            int end = Math.min(i + chunkSize, fullReply.length());
            stepConsumer.accept(toJson(Map.of("type", "reply", "content", fullReply.substring(i, end))));
        }
    }

    @Override
    public void chatWithReActStream(
            String conversationId, String userMessage, int maxSteps,
            java.util.function.Consumer<String> stepConsumer) {
        if (stepConsumer == null) return;
        String id = conversationId != null && !conversationId.isBlank() ? conversationId.trim() : UUID.randomUUID().toString();
        String filtered = filterText(userMessage);
        String input = filtered != null ? filtered : (userMessage != null ? userMessage : "");
        stepConsumer.accept(toJson(Map.of("type", "conv", "conversationId", id)));
        String hint = checkFilteredEmpty(userMessage, input);
        if (hint != null) {
            sendReplyInChunks(hint, stepConsumer);
            return;
        }
        if (maxSteps <= 0) {
            String reply = "maxSteps 必须大于 0。";
            memoryStore.add(id, new UserMessage(input));
            memoryStore.add(id, new AssistantMessage(reply));
            sendReplyInChunks(reply, stepConsumer);
            return;
        }
        List<Message> messages = new ArrayList<>();
        messages.add(new SystemMessage(systemPrompt.replace("{context}", "") + "\n\n" + AMAP_TOOL_GUIDELINES));
        messages.addAll(memoryStore.getMessages(id));
        messages.add(new UserMessage(input));
        ChatOptions toolOptions = null;
        if (reactStreamToolsEnabled) {
            try {
                toolOptions = buildToolCallOptionsForReActStream();
            } catch (Exception e) {
                log.warn("ReAct 流式: 获取工具失败 {}", e.getMessage());
            }
        }
        if (toolOptions == null) {
            if (streamingChatModel != null) {
                StringBuilder sb = new StringBuilder();
                Flux<String> flux = streamingChatModel.stream(new Prompt(messages))
                        .mapNotNull(r -> r != null && r.getResult() != null && r.getResult().getOutput() != null ? r.getResult().getOutput().getText() : null)
                        .filter(s -> s != null && !s.isEmpty())
                        .map(this::filterText);
                flux.doOnNext(chunk -> {
                            sb.append(chunk);
                            stepConsumer.accept(toJson(Map.of("type", "reply", "content", chunk)));
                        })
                        .doOnComplete(() -> {
                            String full = filterText(sb.toString());
                            memoryStore.add(id, new UserMessage(input));
                            memoryStore.add(id, new AssistantMessage(full != null ? full : sb.toString()));
                        })
                        .blockLast();
            } else {
                String reply = filterText(extractReplyText(callModelWithTimeout(new Prompt(messages))));
                memoryStore.add(id, new UserMessage(input));
                memoryStore.add(id, new AssistantMessage(reply != null ? reply : ""));
                sendReplyInChunks(reply, stepConsumer);
            }
            return;
        }
        log.info("---------- ReAct 流式请求开始: message=[{}], maxSteps={} ----------", input, maxSteps);
        int step = 0;
        org.springframework.ai.chat.model.ChatResponse response = null;
        int safeMax = Math.min(maxSteps, 20);
        java.util.List<String> toolsCalled = new ArrayList<>();
        Map<String, ToolCallback> byName = new java.util.HashMap<>();
        for (ToolCallback cb : getMergedToolCallbacksForReActStream()) {
            if (cb.getToolDefinition() != null && cb.getToolDefinition().name() != null)
                byName.put(cb.getToolDefinition().name(), cb);
        }
        try {
            while (step < safeMax) {
                step++;
                log.info("ReAct 流式 步骤 {}/{}", step, safeMax);
                response = callModelWithTimeout(new Prompt(messages, toolOptions));
                Generation gen = response.getResult();
                if (gen == null && response.getResults() != null && !response.getResults().isEmpty()) gen = response.getResults().get(0);
                if (gen == null || gen.getOutput() == null) {
                    log.info("ReAct 流式完成: 共 {} 步（无有效回复），本循环调用工具: {} {}", step, toolsCalled.isEmpty() ? "无" : toolsCalled, toolsCalled.isEmpty() ? "（DashScope 可能在内部已执行工具）" : "");
                    String fb = "抱歉，本轮没有获得有效回复。";
                    memoryStore.add(id, new UserMessage(input));
                    memoryStore.add(id, new AssistantMessage(fb));
                    sendReplyInChunks(fb, stepConsumer);
                    return;
                }
                Message out = gen.getOutput();
                if (out instanceof AssistantMessage am && am.hasToolCalls()) {
                    List<AssistantMessage.ToolCall> tcs = am.getToolCalls();
                    if (tcs == null || tcs.isEmpty()) {
                        log.info("ReAct 流式完成: 共 {} 步（无工具调用），本循环调用工具: {} {}", step, toolsCalled.isEmpty() ? "无" : toolsCalled, toolsCalled.isEmpty() ? "（DashScope 可能在内部已执行工具）" : "");
                        String reply = filterText(am.getText() != null ? am.getText() : "");
                        memoryStore.add(id, new UserMessage(input));
                        memoryStore.add(id, new AssistantMessage(reply != null ? reply : ""));
                        sendReplyInChunks(reply, stepConsumer);
                        return;
                    }
                    if (am.getText() != null && !am.getText().isBlank())
                        stepConsumer.accept(toJson(new ReActStep("thought", am.getText(), null, null)));
                    List<ToolResponseMessage.ToolResponse> trs = new ArrayList<>();
                    for (AssistantMessage.ToolCall tc : tcs) {
                        log.info("  调用工具: {} args={}", tc.name(), tc.arguments() != null ? tc.arguments() : "");
                        toolsCalled.add(tc.name());
                        stepConsumer.accept(toJson(new ReActStep("tool_call", "调用 " + tc.name(), tc.name(), tc.arguments())));
                        ToolCallback cb = byName.get(tc.name());
                        String res = cb != null ? safeCallTool(cb, tc.arguments() != null ? tc.arguments() : "", tc.name()) : "未找到工具";
                        stepConsumer.accept(toJson(new ReActStep("tool_result", res != null ? res : "", null, null)));
                        trs.add(new ToolResponseMessage.ToolResponse(tc.id(), tc.name(), res != null ? res : ""));
                    }
                    messages.add(am);
                    messages.add(new ToolResponseMessage(trs));
                } else {
                    log.info("ReAct 流式完成: 共 {} 步，本循环调用工具: {} {}", step, toolsCalled.isEmpty() ? "无" : toolsCalled, toolsCalled.isEmpty() ? "（DashScope 可能在内部已执行工具）" : "");
                    String reply = filterText(extractReplyText(response));
                    memoryStore.add(id, new UserMessage(input));
                    memoryStore.add(id, new AssistantMessage(reply != null ? reply : ""));
                    sendReplyInChunks(reply, stepConsumer);
                    return;
                }
            }
            log.info("ReAct 流式完成: 共 {} 步（达最大步数），本循环调用工具: {} {}", step, toolsCalled.isEmpty() ? "无" : toolsCalled, toolsCalled.isEmpty() ? "（DashScope 可能在内部已执行工具）" : "");
            String last = response != null ? filterText(extractReplyText(response)) : "";
            if (last == null || last.isBlank()) {
                last = "已达到最大规划步数。";
            }
            memoryStore.add(id, new UserMessage(input));
            memoryStore.add(id, new AssistantMessage(last));
            sendReplyInChunks(last, stepConsumer);
        } catch (Exception e) {
            log.info("ReAct 流式异常结束: 共 {} 步，调用工具: {}", step, toolsCalled.isEmpty() ? "无" : toolsCalled);
            log.error("ReAct 流式执行失败", e);
            String err = "执行错误: " + (e.getMessage() != null ? e.getMessage() : "未知异常");
            stepConsumer.accept(toJson(Map.of("type", "error", "content", err)));
            memoryStore.add(id, new UserMessage(input));
            memoryStore.add(id, new AssistantMessage(err));
        }
    }

    private String safeCallTool(ToolCallback cb, String args, String name) {
        try {
            Object r = java.util.concurrent.CompletableFuture
                    .supplyAsync(() -> cb.call(args))
                    .orTimeout(reactStepTimeoutMs, TimeUnit.MILLISECONDS)
                    .join();
            if (r == null) return "";
            if (r instanceof String s) return s;
            return objectMapper.writeValueAsString(r);
        } catch (Exception e) {
            Throwable root = (e instanceof CompletionException && e.getCause() != null) ? e.getCause() : e;
            log.warn("工具执行异常 {}: {}", name, root.getMessage());
            return "工具执行异常: " + (root.getMessage() != null ? root.getMessage() : root.getClass().getSimpleName());
        }
    }

    private org.springframework.ai.chat.model.ChatResponse callModelWithTimeout(Prompt prompt) {
        try {
            return java.util.concurrent.CompletableFuture
                    .supplyAsync(() -> chatModel.call(prompt))
                    .orTimeout(reactStepTimeoutMs, TimeUnit.MILLISECONDS)
                    .join();
        } catch (Exception e) {
            Throwable root = (e instanceof CompletionException && e.getCause() != null) ? e.getCause() : e;
            String msg = root.getMessage() != null ? root.getMessage() : root.getClass().getSimpleName();
            throw new RuntimeException("模型调用超时或失败: " + msg, root);
        }
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return "{\"type\":\"error\",\"content\":\"序列化失败\"}";
        }
    }
}

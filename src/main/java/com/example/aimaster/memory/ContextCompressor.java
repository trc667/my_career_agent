package com.example.aimaster.memory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 多轮对话上下文管理：token 预算裁剪 + 早期对话 LLM 摘要压缩。
 * <p>
 * 策略：从最新消息向前累加 token，超出预算的早期消息不直接丢弃，
 * 而是调用 LLM 生成要点摘要（按会话缓存复用），以一条 system 消息
 * 占位，最大程度保留关键上下文同时控制每次调用的 token 成本。
 * 摘要生成失败时降级为直接丢弃，保证主链路不中断。
 */
@Component
public class ContextCompressor {

    private static final Logger log = LoggerFactory.getLogger(ContextCompressor.class);

    /** 每条消息除正文外的固定开销（角色标记/分隔符等）估算 */
    private static final int MESSAGE_OVERHEAD = 4;

    private static final String SUMMARY_SYSTEM_PROMPT =
            "你是一个对话摘要助手。请用简洁的中文总结用户与 AI 的历史对话，只保留对后续回答仍有用的关键信息：\n"
            + "1. 用户身份/背景/目标（如求职方向、专业、年级、目标公司类型）；\n"
            + "2. 已讨论的主题与结论、AI 已给出的核心建议；\n"
            + "3. 用户明确表达的偏好、决定、待办事项。\n"
            + "直接输出摘要正文，不要任何前缀、标题或解释性文字。";

    private final ChatModel chatModel;
    private final int historyTokenBudget;
    private final int summaryMaxChars;
    private final boolean enableSummary;

    /**
     * 会话摘要缓存（Caffeine 本地缓存）：
     * - maximumSize 容量上限：按预估最大在线会话数设置，防内存无限上涨 OOM；
     * - expireAfterAccess 空闲过期：会话持续使用自动续期，长时间不用自动清理，防旧会话堆积；
     * - recordStats 开启命中率统计（供监控）。
     */
    private final Cache<String, CachedSummary> summaryCache;

    /** 裁剪结果：保留的最近消息 + 早期对话摘要（可为空） */
    public record PreparedHistory(List<Message> keptMessages, String summary) {
    }

    private record CachedSummary(String summary, int messageCount) {
    }

    public ContextCompressor(ChatModel chatModel,
                             @Value("${app.memory.history-token-budget:4000}") int historyTokenBudget,
                             @Value("${app.memory.summary-max-chars:800}") int summaryMaxChars,
                             @Value("${app.memory.enable-summary:true}") boolean enableSummary,
                             @Value("${app.memory.summary-cache-max-size:500}") int summaryCacheMaxSize,
                             @Value("${app.memory.summary-cache-ttl-minutes:360}") long summaryCacheTtlMinutes) {
        this.chatModel = chatModel;
        this.historyTokenBudget = Math.max(200, historyTokenBudget);
        this.summaryMaxChars = Math.max(100, summaryMaxChars);
        this.enableSummary = enableSummary;
        this.summaryCache = Caffeine.newBuilder()
                .maximumSize(Math.max(10, summaryCacheMaxSize))
                .expireAfterAccess(Math.max(10, summaryCacheTtlMinutes), TimeUnit.MINUTES)
                .recordStats()
                .build();
    }

    /** 估算文本 token 数：中文约 1 token ≈ 1.6 字符（qwen 分词近似，预算裁剪取保守值） */
    public int estimateTokens(String text) {
        if (text == null || text.isEmpty()) return 0;
        return (int) Math.ceil(text.length() / 1.6);
    }

    /**
     * 按 token 预算裁剪历史：保留最近消息（至少 1 条），超预算的早期消息
     * 优先用摘要占位（缓存复用/合并），摘要不可用时直接丢弃。
     */
    public PreparedHistory prepare(String conversationId, List<Message> history) {
        if (history == null || history.isEmpty()) {
            return new PreparedHistory(List.of(), null);
        }
        // 从最新往前累加 token，找出可保留的起始下标
        int keepFrom = history.size();
        int acc = 0;
        for (int i = history.size() - 1; i >= 0; i--) {
            int t = estimateTokens(getText(history.get(i))) + MESSAGE_OVERHEAD;
            if (acc + t > historyTokenBudget && keepFrom < history.size()) {
                break;
            }
            acc += t;
            keepFrom = i;
        }
        List<Message> kept = history.subList(keepFrom, history.size());
        List<Message> dropped = history.subList(0, keepFrom);
        if (dropped.isEmpty() || !enableSummary) {
            if (!dropped.isEmpty()) {
                log.info("上下文预算裁剪: 会话 {} 超预算，丢弃 {} 条早期消息（摘要压缩已关闭）", conversationId, dropped.size());
            }
            return new PreparedHistory(kept, null);
        }
        log.info("上下文预算裁剪: 会话 {} 历史 {} 条，保留最近 {} 条，丢弃 {} 条需压缩",
                conversationId, history.size(), kept.size(), dropped.size());
        // 摘要缓存：本次丢弃完全被旧摘要覆盖则直接复用
        CachedSummary cached = summaryCache.getIfPresent(conversationId);
        if (cached != null && cached.messageCount() >= dropped.size()) {
            log.info("复用会话摘要缓存: 会话 {}（覆盖 {} 条，摘要 {} 字）", conversationId, cached.messageCount(), cached.summary().length());
            return new PreparedHistory(kept, cached.summary());
        }
        // 合并：旧摘要文本 + 未被旧摘要覆盖的新丢弃消息，一起生成新摘要
        List<Message> toSummarize = new ArrayList<>();
        int alreadyCovered = cached != null ? cached.messageCount() : 0;
        if (cached != null && !cached.summary().isBlank()) {
            toSummarize.add(new UserMessage("（前文摘要）" + cached.summary()));
        }
        if (alreadyCovered < dropped.size()) {
            toSummarize.addAll(dropped.subList(alreadyCovered, dropped.size()));
        }
        String summary = summarize(toSummarize);
        if (summary == null || summary.isBlank()) {
            // 压缩失败：尽量保留旧摘要，否则降级为直接丢弃
            return new PreparedHistory(kept, cached != null ? cached.summary() : null);
        }
        summaryCache.put(conversationId, new CachedSummary(summary, dropped.size()));
        log.info("已生成对话摘要: 会话 {}，覆盖 {} 条历史，摘要 {} 字", conversationId, dropped.size(), summary.length());
        return new PreparedHistory(kept, summary);
    }

    /** 调用 LLM 生成对话要点摘要；失败或内容过短返回 null（由调用方降级） */
    public String summarize(List<Message> messages) {
        if (messages == null || messages.isEmpty()) return null;
        String text = messages.stream()
                .map(ContextCompressor::getText)
                .filter(s -> s != null && !s.isBlank())
                .collect(Collectors.joining("\n"));
        if (text.isBlank()) return null;
        // 内容本身很短的对话不值得压缩（避免无谓 LLM 调用）
        if (estimateTokens(text) < 200) return null;
        try {
            String userPrompt = "请用最多 " + summaryMaxChars + " 字总结以下历史对话的要点：\n\n" + text;
            org.springframework.ai.chat.model.ChatResponse resp = chatModel.call(
                    new Prompt(List.of(new SystemMessage(SUMMARY_SYSTEM_PROMPT), new UserMessage(userPrompt))));
            if (resp == null || resp.getResult() == null || resp.getResult().getOutput() == null) return null;
            String raw = resp.getResult().getOutput().getText();
            if (raw == null || raw.isBlank()) return null;
            if (raw.length() > summaryMaxChars) raw = raw.substring(0, summaryMaxChars);
            return raw.trim();
        } catch (Exception e) {
            log.warn("对话摘要生成失败，降级为直接丢弃旧消息: {}", e.getMessage());
            return null;
        }
    }

    /** 清空某会话的摘要缓存（会话删除时调用，避免残留） */
    public void evict(String conversationId) {
        if (conversationId != null) {
            summaryCache.invalidate(conversationId);
        }
    }

    /** 缓存统计（命中率/计数，供监控面板接入） */
    public com.github.benmanes.caffeine.cache.stats.CacheStats cacheStats() {
        return summaryCache.stats();
    }

    private static String getText(Message m) {
        if (m == null) return "";
        try {
            String t = m.getText();
            return t != null ? t : "";
        } catch (Exception e) {
            if (m instanceof UserMessage u) return u.getText() != null ? u.getText() : "";
            if (m instanceof AssistantMessage a) return a.getText() != null ? a.getText() : "";
            return "";
        }
    }
}

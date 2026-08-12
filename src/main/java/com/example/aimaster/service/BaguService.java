package com.example.aimaster.service;

import com.example.aimaster.rag.RagDocumentLoader;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * 八股知识库服务：启动时加载 career-tips.txt 到内存缓存，提供分类/搜索/分页/随机抽题。
 * <p>
 * 设计（面试可讲）：
 * 1) 内存缓存：629 段知识库启动时一次加载（毫秒级），避免每次请求读文件；
 * 2) 复用 RagDocumentLoader.autoTag 关键词分类器打标签（零重复代码）；
 * 3) 随机抽题用 java.util.Random + 分类内 shuffle，供碎片时间刷题。
 */
@Slf4j
@Service
public class BaguService {

    /** 一条八股知识点（id 为内容 SHA-256 前 12 位 hex，内容不变则 id 稳定，供错题本引用） */
    public record BaguEntry(String id, String content, String category) {
        /** 列表摘要：取前 60 字 */
        public String summary() {
            return content.length() > 60 ? content.substring(0, 60) + "…" : content;
        }
    }

    /** 分页结果 */
    public record BaguPage(List<BaguEntry> list, long total) {
    }

    /** 随机一题结果：question 为 LLM 改写后的疑问句，content 为原文知识点（供 AI 讲解/参考） */
    public record QuestionEntry(String id, String question, String content, String category) {
    }

    /** 抽题后 LLM 改写 Prompt：把陈述句知识点转成可直接作答的八股问句 */
    private static final String QUESTION_REWRITE_PROMPT =
            "你是一位资深技术面试官，正在出八股文面试题。\n"
            + "下面是一条技术知识点，请把它改写成一道简短、可直接作答的面试问句：\n"
            + "1) 以问号结尾，口语化，引导解释原理/原因/区别；\n"
            + "2) 不要包含答案内容，保持简洁（不超过 50 字）；\n"
            + "3) 若原文已是问句则润色后保留。\n"
            + "知识点：{knowledge}\n"
            + "只输出改写后的问题本身，不要输出其他任何文字。";

    private volatile List<BaguEntry> entries = new ArrayList<>();
    private final Random random = new Random();
    private final ChatModel chatModel;

    public BaguService(ChatModel chatModel) {
        this.chatModel = chatModel;
        load();
    }

    /** 加载知识库文件：按双换行分段，过滤注释/空段，autoTag 分类 */
    private void load() {
        try {
            String content = new ClassPathResource("rag/career-tips.txt")
                    .getContentAsString(StandardCharsets.UTF_8);
            reload(RagDocumentLoader.splitParagraphs(content));
        } catch (Exception e) {
            log.error("八股知识库加载失败：{}", e.getMessage());
        }
    }

    /** 重建八股内存缓存（知识库管理入口变更后调用）：以启用知识段列表替换旧缓存 */
    public void reload(List<String> paragraphs) {
        if (paragraphs == null) return;
        List<BaguEntry> rebuilt = new ArrayList<>(paragraphs.size());
        for (String s : paragraphs) {
            if (s == null || s.isBlank()) continue;
            rebuilt.add(new BaguEntry(hashId(s), s, RagDocumentLoader.autoTag(s)));
        }
        entries = rebuilt;
        log.info("八股知识库重建完成：{} 段", entries.size());
    }

    /** 分类 + 关键词过滤 + 分页 */
    public BaguPage list(String category, String keyword, int page, int size) {
        List<BaguEntry> filtered = entries.stream()
                .filter(e -> category == null || category.isBlank() || e.category().equals(category))
                .filter(e -> keyword == null || keyword.isBlank() || e.content().contains(keyword))
                .toList();
        int total = filtered.size();
        int from = Math.max(0, page * size);
        int to = Math.min(total, from + size);
        List<BaguEntry> pageList = from >= total ? List.of() : filtered.subList(from, to);
        return new BaguPage(pageList, total);
    }

    /** 各分类条目统计（用于前端标签页） */
    public List<Map<String, Object>> categories() {
        Map<String, Integer> countMap = new LinkedHashMap<>();
        for (BaguEntry e : entries) {
            countMap.merge(e.category(), 1, Integer::sum);
        }
        List<Map<String, Object>> result = new ArrayList<>();
        countMap.forEach((cat, cnt) -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("category", cat);
            m.put("count", cnt);
            result.add(m);
        });
        return result;
    }

    /** 稳定 ID：SHA-256 前 12 位 hex（内容不变则 id 不变） */
    static String hashId(String text) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(text.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 6; i++) { // 6 字节 = 12 hex
                sb.append(String.format("%02x", digest[i]));
            }
            return sb.toString();
        } catch (Exception e) {
            return Integer.toHexString(text.hashCode());
        }
    }

    /** 随机抽题：可限定分类；无候选时返回 null */
    public BaguEntry random(String category) {
        List<BaguEntry> pool = category == null || category.isBlank()
                ? entries
                : entries.stream().filter(e -> e.category().equals(category)).toList();
        if (pool.isEmpty()) return null;
        return pool.get(random.nextInt(pool.size()));
    }

    /** 随机抽题并 LLM 改写为疑问句（碎片时间刷题用）；无候选返回 null，改写失败降级原文 */
    public QuestionEntry randomQuestion(String category) {
        BaguEntry e = random(category);
        if (e == null) return null;
        return new QuestionEntry(e.id(), toQuestion(e.content()), e.content(), e.category());
    }

    /** 单条知识点 LLM 改写为问句：非问句结果/调用失败均降级返回原文，保证可用 */
    private String toQuestion(String content) {
        try {
            String prompt = QUESTION_REWRITE_PROMPT.replace("{knowledge}", content);
            org.springframework.ai.chat.model.ChatResponse resp = chatModel.call(new Prompt(prompt));
            String text = resp.getResult() == null ? "" : resp.getResult().getOutput().getText();
            String q = text == null ? "" : text.trim().replaceAll("^[\"'\\u300c\\u300d]+|[\"'\\u300c\\u300d]+$", "");
            if (!q.isEmpty() && (q.endsWith("？") || q.endsWith("?"))) {
                return q;
            }
            log.warn("八股题改写结果非问句，降级原文: {}", q);
            return content;
        } catch (Exception e) {
            log.warn("八股题改写失败，降级原文: {}", e.getMessage());
            return content;
        }
    }

    public int size() {
        return entries.size();
    }
}

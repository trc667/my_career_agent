package com.example.aimaster.service;

import com.example.aimaster.rag.RagDocumentLoader;
import lombok.extern.slf4j.Slf4j;
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

    private final List<BaguEntry> entries = new ArrayList<>();
    private final Random random = new Random();

    public BaguService() {
        load();
    }

    /** 加载知识库文件：按双换行分段，过滤注释/空段，autoTag 分类 */
    private void load() {
        try {
            String content = new ClassPathResource("rag/career-tips.txt")
                    .getContentAsString(StandardCharsets.UTF_8);
            for (String part : content.split("\\n\\s*\\n")) {
                String s = part.trim();
                if (s.isEmpty() || s.startsWith("#")) continue;
                entries.add(new BaguEntry(hashId(s), s, RagDocumentLoader.autoTag(s)));
            }
            log.info("八股知识库加载完成：{} 段", entries.size());
        } catch (Exception e) {
            log.error("八股知识库加载失败：{}", e.getMessage());
        }
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

    public int size() {
        return entries.size();
    }
}

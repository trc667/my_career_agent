package com.example.aimaster.service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 语义缓存：相同/高度相似问题直接命中缓存答案，跳过 RAG 检索与 LLM 调用（进一步省 token、降延迟）。
 * <p>
 * 设计（面试可讲）：
 * 1) 只缓存「新会话首轮、无历史上下文」的独立问题，避免多轮上下文错配导致答非所问；
 * 2) 命中判定：归一化（去标点空白转小写）后精确相等，避免同义改写重复计费；
 * 3) 并发安全：ConcurrentHashMap + 容量上限（超出清掉最旧） + TTL 过期，防止无限增长；
 * 4) 与 FAQ 拦截分层：FAQ 是产品类固定问答（人工维护），语义缓存是自由问答的运行时记忆。
 */
@Component
public class SemanticCache {

    private static final Logger log = LoggerFactory.getLogger(SemanticCache.class);

    /** 缓存条目：答案 + 写入时间戳 */
    private record CacheEntry(String answer, long createdAt) {
    }

    private final int capacity;
    private final long ttlMillis;

    /** query 归一化 → 缓存条目 */
    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();

    public SemanticCache(@Value("${app.semantic-cache.capacity:200}") int capacity,
                         @Value("${app.semantic-cache.ttl-seconds:1800}") long ttlSeconds) {
        this.capacity = Math.max(10, capacity);
        this.ttlMillis = Math.max(30, ttlSeconds) * 1000L;
    }

    /**
     * 命中返回缓存答案，未命中/过期返回 null。
     * 命中后刷新时间戳（滑动 TTL：常用问题长期有效）。
     */
    public String get(String question) {
        if (question == null || question.isBlank()) return null;
        String key = normalize(question);
        CacheEntry entry = cache.get(key);
        if (entry == null) return null;
        long now = System.currentTimeMillis();
        if (now - entry.createdAt() > ttlMillis) {
            cache.remove(key, entry);
            return null;
        }
        // 滑动 TTL：命中即刷新
        cache.put(key, new CacheEntry(entry.answer(), now));
        log.info("语义缓存命中：{} 字", entry.answer().length());
        return entry.answer();
    }

    /** 写入缓存：容量满时淘汰最旧条目（按 createdAt 排序删 1/4） */
    public void put(String question, String answer) {
        if (question == null || question.isBlank() || answer == null || answer.isBlank()) return;
        if (cache.size() >= capacity) {
            evictOldest();
        }
        cache.put(normalize(question), new CacheEntry(answer, System.currentTimeMillis()));
        log.info("语义缓存写入：key={}", normalize(question));
    }

    /** 清空缓存（管理接口可选调用） */
    public void clear() {
        cache.clear();
        log.info("语义缓存已清空");
    }

    public int size() {
        return cache.size();
    }

    private void evictOldest() {
        long oldestTs = Long.MAX_VALUE;
        String oldestKey = null;
        for (Map.Entry<String, CacheEntry> e : cache.entrySet()) {
            if (e.getValue().createdAt() < oldestTs) {
                oldestTs = e.getValue().createdAt();
                oldestKey = e.getKey();
            }
        }
        if (oldestKey != null) cache.remove(oldestKey);
    }

    /** 归一化：去标点、空白、emoji，转小写（与 FaqService 一致，保证行为可预期） */
    static String normalize(String s) {
        if (s == null) return "";
        return s.replaceAll("[\\p{P}\\p{S}\\s]", "").toLowerCase(java.util.Locale.ROOT);
    }
}

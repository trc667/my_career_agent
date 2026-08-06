package com.example.aimaster.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 语义缓存：相同/高度相似问题直接命中缓存答案，跳过 RAG 检索与 LLM 调用（进一步省 token、降延迟）。
 * <p>
 * 实现：基于 Caffeine 本地缓存（替代手写 ConcurrentHashMap + 手写 TTL/容量淘汰）：
 * - maximumSize 固定容量上限（默认 200），超出由 Caffeine 按 W-TinyLFU 淘汰高频不常用的；
 * - expireAfterWrite 写入后 30 分钟过期，过期的读会自动清理，无内存残留；
 * - recordStats 开启命中率统计，供监控判断缓存效果。
 * <p>
 * 设计（面试可讲）：
 * 1) 只缓存「新会话首轮、无历史上下文」的独立问题，避免多轮上下文错配导致答非所问；
 * 2) 命中判定：归一化（去标点空白转小写）后精确相等；
 * 3) 与 FAQ 拦截分层：FAQ 是产品类固定问答（人工维护），语义缓存是自由问答的运行时记忆。
 * 4) 回源（LLM 调用）由调用方显式控制（命中才返回，未命中走主链路并决定是否 put），
 *    不回源逻辑放进 Caffeine loader，避免昂贵调用被锁在缓存内部。
 */
@Component
public class SemanticCache {

    private static final Logger log = LoggerFactory.getLogger(SemanticCache.class);

    /** 归一化问题 → 答案（Caffeine：容量上限 + 写入后过期 + 统计） */
    private final Cache<String, String> cache;

    public SemanticCache(@Value("${app.semantic-cache.capacity:200}") int capacity,
                         @Value("${app.semantic-cache.ttl-seconds:1800}") long ttlSeconds) {
        this.cache = Caffeine.newBuilder()
                .maximumSize(Math.max(10, capacity))
                .expireAfterWrite(Math.max(30, ttlSeconds), TimeUnit.SECONDS)
                .recordStats()
                .build();
    }

    /** 命中返回缓存答案，未命中/过期返回 null（过期条目由 Caffeine 惰性清理） */
    public String get(String question) {
        if (question == null || question.isBlank()) return null;
        String answer = cache.getIfPresent(normalize(question));
        if (answer != null) {
            log.info("语义缓存命中：{} 字", answer.length());
        }
        return answer;
    }

    /** 写入缓存（容量/过期由 Caffeine 管理，无需手动淘汰） */
    public void put(String question, String answer) {
        if (question == null || question.isBlank() || answer == null || answer.isBlank()) return;
        cache.put(normalize(question), answer);
        log.info("语义缓存写入：key={}", normalize(question));
    }

    /** 清空缓存（管理接口可选调用） */
    public void clear() {
        cache.invalidateAll();
        log.info("语义缓存已清空");
    }

    public long size() {
        return cache.estimatedSize();
    }

    /** 缓存统计（命中率/计数，供监控面板接入） */
    public com.github.benmanes.caffeine.cache.stats.CacheStats cacheStats() {
        return cache.stats();
    }

    /** 归一化：去标点、空白、emoji，转小写（与 FaqService 一致，保证行为可预期） */
    static String normalize(String s) {
        if (s == null) return "";
        return s.replaceAll("[\\p{P}\\p{S}\\s]", "").toLowerCase(java.util.Locale.ROOT);
    }
}

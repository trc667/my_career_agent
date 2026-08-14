package com.example.aimaster.service;

import java.time.LocalDate;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 游客试用限制：未登录用户每日可免费试用 LLM 对话 N 次（按 IP 计数，内存实现）。
 * <p>
 * 设计（面试可讲）：
 * 1) 漏斗闭环：游客零门槛体验 → 用完引导注册（转化漏斗顶部）；
 * 2) 成本护栏：每日次数上限 + 分级频率限流（游客 10 次/分）双重防护，防止匿名刷 LLM 成本；
 * 3) 内存计数 key=IP，值为 {日期, 当日已用次数}，跨日自动重置；
 * 4) 周期清理跨日 key，防止 IP 无限增长导致内存膨胀。
 */
@Service
public class GuestTrialService {

    private static final Logger log = LoggerFactory.getLogger(GuestTrialService.class);

    /** 游客每日试用次数上限 */
    public static final int DAILY_LIMIT = 3;
    /** 每处理多少次请求触发一次过期 key 清理 */
    private static final int CLEANUP_EVERY = 500;

    private static class Usage {
        final LocalDate date;
        int count;

        Usage(LocalDate date, int count) {
            this.date = date;
            this.count = count;
        }
    }

    private final ConcurrentHashMap<String, Usage> usages = new ConcurrentHashMap<>();
    private final AtomicLong hits = new AtomicLong();

    /**
     * 尝试消耗一次游客试用额度（原子计数）。
     *
     * @param ip 客户端 IP（可空，空时视为内网/不可识别，不限制）
     * @return null=放行；非空=超限提示文案（调用方直接返回给前端）
     */
    public String tryConsume(String ip) {
        if (ip == null || ip.isBlank()) {
            return null;
        }
        LocalDate today = LocalDate.now();
        Usage u = usages.compute(ip, (k, old) -> {
            if (old == null || !today.equals(old.date)) {
                return new Usage(today, 1);
            }
            old.count++;
            return old;
        });
        maybeCleanup();
        if (u.count > DAILY_LIMIT) {
            log.info("游客试用超限: ip={} 今日已用={}", ip, u.count);
            return "游客每日可试用 " + DAILY_LIMIT + " 次，注册登录后无限畅聊";
        }
        return null;
    }

    /** 周期性清理跨日的 key，防止匿名 IP 无限堆积 */
    private void maybeCleanup() {
        if (hits.incrementAndGet() % CLEANUP_EVERY != 0) {
            return;
        }
        LocalDate today = LocalDate.now();
        usages.entrySet().removeIf(e -> !today.equals(e.getValue().date));
    }
}

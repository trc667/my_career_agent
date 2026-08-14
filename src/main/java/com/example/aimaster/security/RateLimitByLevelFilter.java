package com.example.aimaster.security;

import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.aimaster.entity.User;
import com.example.aimaster.mapper.UserMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * 分级限流过滤器：按会员等级分配对话频率，替代静态的 Resilience4j 统一限流。
 * <p>
 * 设计（面试可讲）：
 * 1) 滑动窗口计数（ConcurrentHashMap + Deque），非固定窗口，突发更平滑；
 * 2) 等级区分：游客 10 次/分、FREE 20 次/分、VIP 60 次/分、ADMIN 不限流；
 *    为商业化"免费引流、付费提速"提供基础设施；
 * 3) key 维度：登录用户按 username，游客按 IP（防止换账号绕过）；
 * 4) 只作用于 /api/chat*（LLM 最贵路径），管理/登录等接口不在此限。
 * <p>
 * 注意：原 Resilience4j chatLimiter 保留为全局兜底（本过滤器先判定，再走 Spring 过滤器链）。
 */
@Component
public class RateLimitByLevelFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitByLevelFilter.class);

    /** 滑动窗口（毫秒）：1 分钟 */
    private static final long WINDOW_MS = 60_000L;

    /** 游客 / FREE / VIP 每分钟请求上限 */
    private static final int LIMIT_ANON = 10;
    private static final int LIMIT_FREE = 20;
    private static final int LIMIT_VIP = 60;
    /** ADMIN 不限流 */
    private static final int LIMIT_UNLIMITED = -1;

    /** 每处理多少次请求触发一次窗口清理 */
    private static final int CLEANUP_EVERY = 1000;

    private final UserMapper userMapper;

    /** key(username 或 ip) → 最近请求时间戳队列 */
    private final Map<String, Deque<Long>> hits = new ConcurrentHashMap<>();

    /** 请求计数：周期性清理已过期的 key，防止 map 随唯一 IP/用户名无限增长 */
    private final AtomicLong requestCount = new AtomicLong();

    public RateLimitByLevelFilter(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // 只限职规大师/超级智能体聊天路径（LLM 最贵），其余不限
        String path = request.getRequestURI();
        return path == null || !path.startsWith("/api/chat");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        int limit = resolveLimit();
        if (limit == LIMIT_UNLIMITED) {
            chain.doFilter(request, response);
            return;
        }
        maybeCleanup();
        String key = resolveKey(request);
        long now = System.currentTimeMillis();
        Deque<Long> queue = hits.computeIfAbsent(key, k -> new ArrayDeque<>());
        synchronized (queue) {
            // 清掉窗口外的旧记录
            while (!queue.isEmpty() && now - queue.peekFirst() > WINDOW_MS) {
                queue.pollFirst();
            }
            if (queue.size() >= limit) {
                log.warn("分级限流触发: key={} limit={}/min", key, limit);
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.setContentType(MediaType.APPLICATION_JSON_VALUE + ";charset=" + StandardCharsets.UTF_8);
                response.getWriter().write("{\"code\":429,\"message\":\"请求过于频繁，请稍后再试\"}");
                return;
            }
            queue.addLast(now);
        }
        chain.doFilter(request, response);
    }

    /** 周期性清理：移除窗口内已空的 key，避免匿名 IP/用户 key 无限堆积 */
    private void maybeCleanup() {
        if (requestCount.incrementAndGet() % CLEANUP_EVERY != 0) {
            return;
        }
        long now = System.currentTimeMillis();
        hits.entrySet().removeIf(entry -> {
            Deque<Long> q = entry.getValue();
            synchronized (q) {
                while (!q.isEmpty() && now - q.peekFirst() > WINDOW_MS) {
                    q.pollFirst();
                }
                return q.isEmpty();
            }
        });
    }

    /** 按当前登录用户等级返回限流值；未登录按游客；ADMIN 返回 -1 不限 */
    private int resolveLimit() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getPrincipal() == null
                || "anonymousUser".equals(auth.getPrincipal().toString())) {
            return LIMIT_ANON;
        }
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, auth.getName()));
        if (user == null) return LIMIT_ANON;
        if ("ADMIN".equals(user.getRole())) return LIMIT_UNLIMITED;
        return "VIP".equals(user.getLevel()) ? LIMIT_VIP : LIMIT_FREE;
    }

    /** 登录用户按 username，游客按 IP（防换账号绕过） */
    private String resolveKey(HttpServletRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() != null
                && !"anonymousUser".equals(auth.getPrincipal().toString())) {
            return "u:" + auth.getName();
        }
        String ip = request.getRemoteAddr();
        return "a:" + (ip != null ? ip : "unknown");
    }
}

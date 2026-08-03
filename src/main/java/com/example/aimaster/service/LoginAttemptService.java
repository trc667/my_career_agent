package com.example.aimaster.service;

import com.example.aimaster.exception.BusinessException;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 登录防爆破：连续失败 5 次锁定账号 15 分钟（内存存储，单机够用）。
 */
@Service
public class LoginAttemptService {

    private static final int MAX_ATTEMPTS = 5;
    private static final long LOCK_DURATION_MS = 15 * 60 * 1000L; // 15 分钟

    private static class Attempt {
        int count;
        long lockUntil;
    }

    private final ConcurrentHashMap<String, Attempt> attempts = new ConcurrentHashMap<>();

    /** 若账号被锁定则抛业务异常 */
    public void checkLocked(String username) {
        String key = username == null ? "" : username.toLowerCase();
        Attempt a = attempts.get(key);
        if (a != null && a.lockUntil > System.currentTimeMillis()) {
            throw new BusinessException("失败次数过多，账号已锁定，请 15 分钟后再试");
        }
    }

    /** 记录一次登录失败；达到阈值则锁定 */
    public void recordFailure(String username) {
        String key = username == null ? "" : username.toLowerCase();
        long now = System.currentTimeMillis();
        attempts.compute(key, (k, a) -> {
            if (a == null || now >= a.lockUntil) {
                a = new Attempt();
            }
            a.count++;
            if (a.count >= MAX_ATTEMPTS) {
                a.lockUntil = now + LOCK_DURATION_MS;
            }
            return a;
        });
    }

    /** 登录成功清除记录 */
    public void reset(String username) {
        if (username != null) {
            attempts.remove(username.toLowerCase());
        }
    }
}

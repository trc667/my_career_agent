package com.example.aimaster.service;

import com.example.aimaster.exception.BusinessException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 邮箱验证码服务：生成/发送/校验验证码（内存存储，5 分钟有效，60 秒冷却防刷）。
 */
@Service
public class EmailCodeService {

    private static final Logger log = LoggerFactory.getLogger(EmailCodeService.class);
    private static final SecureRandom RANDOM = new SecureRandom();
    /** 验证码有效期（毫秒）：5 分钟 */
    private static final long CODE_EXPIRE_MS = 5 * 60 * 1000L;
    /** 同一邮箱重发冷却（毫秒）：60 秒 */
    private static final long RESEND_COOLDOWN_MS = 60 * 1000L;

    private static class CodeEntry {
        final String code;
        final long expiry;
        final long lastSentAt;

        CodeEntry(String code, long expiry, long lastSentAt) {
            this.code = code;
            this.expiry = expiry;
            this.lastSentAt = lastSentAt;
        }
    }

    private final JavaMailSender mailSender;
    private final ConcurrentHashMap<String, CodeEntry> codes = new ConcurrentHashMap<>();
    /** IP 级限流：每 IP 每小时最多发送次数 */
    private static final int IP_MAX_PER_HOUR = 10;
    private static final long IP_WINDOW_MS = 60 * 60 * 1000L;
    private final ConcurrentHashMap<String, int[]> ipCounts = new ConcurrentHashMap<>(); // ip -> [count, windowStartSec]

    /** 发件人地址（spring.mail.username），QQ 邮箱要求 MAIL FROM 与授权用户一致 */
    @Value("${spring.mail.username:}")
    private String mailFrom;

    public EmailCodeService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    /** IP 频率限制：每 IP 每小时最多 IP_MAX_PER_HOUR 次 */
    private void checkIpLimit(String ip) {
        if (ip == null || ip.isBlank()) return;
        long nowSec = System.currentTimeMillis() / 1000;
        int[] v = ipCounts.compute(ip, (k, old) -> {
            if (old == null || nowSec - old[1] >= IP_WINDOW_MS / 1000) {
                return new int[]{1, (int) nowSec};
            }
            old[0]++;
            return old;
        });
        if (v[0] > IP_MAX_PER_HOUR) {
            throw new BusinessException("发送过于频繁，请 1 小时后再试");
        }
    }

    /** 是否可发送（未在冷却期内） */
    private boolean canSend(String email) {
        CodeEntry entry = codes.get(email);
        return entry == null || System.currentTimeMillis() - entry.lastSentAt >= RESEND_COOLDOWN_MS;
    }

    /**
     * 发送验证码到指定邮箱（默认注册场景）。
     * 冷却期内重复调用抛业务异常；发送失败抛业务异常。
     *
     * @param email 目标邮箱
     * @param ip    客户端 IP（用于频率限制，可空）
     */
    public void sendCode(String email, String ip) {
        sendCode(email, ip, "register");
    }

    /**
     * 发送验证码到指定邮箱（可指定场景：register 注册 / forgot 找回密码）。
     * 同一邮箱的验证码池共用（都用于证明邮箱所有权，场景间天然隔离：注册邮箱必未注册、找回邮箱必有用户）。
     */
    public void sendCode(String email, String ip, String scene) {
        checkIpLimit(ip);
        String key = email.toLowerCase().trim();
        if (!canSend(key)) {
            throw new BusinessException("发送过于频繁，请 60 秒后再试");
        }
        String code = String.format("%06d", RANDOM.nextInt(1_000_000));
        long now = System.currentTimeMillis();
        codes.put(key, new CodeEntry(code, now + CODE_EXPIRE_MS, now));

        String sceneName = "forgot".equals(scene) ? "找回密码" : "注册";
        try {
            MimeMessage mime = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mime, "UTF-8");
            if (mailFrom != null && !mailFrom.isBlank()) {
                helper.setFrom(mailFrom);
            }
            helper.setTo(key);
            helper.setSubject("【AI 职规助手】" + sceneName + "验证码");
            helper.setText(buildHtmlCodeEmail(code, sceneName), true);
            mailSender.send(mime);
            log.info("{}验证码已发送至 {}", sceneName, key);
        } catch (Exception e) {
            codes.remove(key);
            log.warn("{}验证码发送失败 {}: {}", sceneName, key, e.getMessage());
            throw new BusinessException("邮件发送失败，请检查邮箱地址或稍后重试");
        }
    }

    /** 校验验证码：正确则消耗（删除），失败抛业务异常 */
    public void validate(String email, String code) {
        String key = email == null ? "" : email.toLowerCase().trim();
        CodeEntry entry = codes.get(key);
        if (entry == null || System.currentTimeMillis() > entry.expiry) {
            codes.remove(key);
            throw new BusinessException("验证码已过期，请重新获取");
        }
        if (!entry.code.equals(code == null ? "" : code.trim())) {
            throw new BusinessException("验证码错误");
        }
        codes.remove(key);
    }

    /** 构建 HTML 验证码邮件（内联样式，兼容主流邮件客户端），sceneName 用于区分注册/找回密码文案 */
    private String buildHtmlCodeEmail(String code, String sceneName) {
        String actionDesc = "找回密码".equals(sceneName)
                ? "您正在重置 <b style=\"color:#409eff;\">AI 职规助手</b> 的登录密码，本次的验证码是："
                : "您正在注册 <b style=\"color:#409eff;\">AI 职规助手</b>，本次的邮箱验证码是：";
        return """
            <div style="max-width:480px;margin:0 auto;padding:24px;font-family:'PingFang SC','Microsoft YaHei','Helvetica Neue',Arial,sans-serif;background:#f0f2f5;border-radius:16px;">
              <div style="text-align:center;padding:8px 0 20px;">
                <div style="display:inline-block;width:48px;height:48px;line-height:48px;border-radius:14px;background:linear-gradient(135deg,#409eff,#5db2ff);color:#ffffff;font-size:20px;font-weight:bold;box-shadow:0 4px 14px rgba(64,158,255,.35);">AI</div>
                <div style="font-size:18px;font-weight:bold;color:#303133;margin-top:10px;">🎓 AI 职规助手</div>
              </div>
              <div style="background:#ffffff;border-radius:12px;padding:28px 24px;box-shadow:0 8px 24px rgba(0,0,0,.05);">
                <div style="font-size:14px;color:#606266;">"""
                + actionDesc
                + """
                </div>
                <div style="text-align:center;margin:20px 0;padding:16px;background:#f0f7ff;border:1px dashed rgba(64,158,255,.4);border-radius:10px;">
                  <span style="font-size:38px;font-weight:bold;color:#409eff;letter-spacing:10px;font-family:Consolas,Menlo,monospace;">
                """
                + code
                + """
                  </span>
                </div>
                <div style="font-size:13px;color:#909399;line-height:1.9;">
                  ⏱ 验证码 <b>5 分钟内有效</b>，请勿泄露给他人。<br/>
                  🔒 若非本人操作，请忽略本邮件，并注意账号安全。
                </div>
              </div>
              <div style="text-align:center;font-size:12px;color:#c0c4cc;margin-top:18px;">© AI 职规助手 · 本邮件由系统自动发送，请勿直接回复</div>
            </div>
            """;
    }
}

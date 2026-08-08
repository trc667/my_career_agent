package com.example.aimaster.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.example.aimaster.entity.PointLog;
import com.example.aimaster.entity.SignIn;
import com.example.aimaster.entity.User;
import com.example.aimaster.exception.BusinessException;
import com.example.aimaster.mapper.PointLogMapper;
import com.example.aimaster.mapper.SignInMapper;
import com.example.aimaster.mapper.UserMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 积分/会员服务：签到、积分变更（流水可审计）、连续天数、VIP 到期回落。
 * <p>
 * 设计（面试可讲）：
 * 1) 幂等签到：sign_in 表 (user_id, sign_date) 唯一键 + 先查后插，同一天重复签到不会加分；
 * 2) 连续天数：从今天（已签则从今天，否则从昨天）往前数连续签到记录，第 7 天有额外奖励；
 * 3) 积分变更统一走 addPoints：更新余额 + 写 point_log 流水，@Transactional 保证原子性；
 * 4) VIP 懒回落：查询时发现过期则回 FREE（无需定时任务，简单可靠）。
 */
@Service
public class PointService {

    private static final Logger log = LoggerFactory.getLogger(PointService.class);

    /** 每日签到基础积分 */
    private static final int SIGN_IN_POINTS = 5;
    /** 连续签到第 7 天（及倍数）额外奖励 */
    private static final int STREAK_BONUS_POINTS = 10;
    /** 连续签到奖励周期（天） */
    private static final int STREAK_CYCLE = 7;

    private final UserMapper userMapper;
    private final PointLogMapper pointLogMapper;
    private final SignInMapper signInMapper;

    public PointService(UserMapper userMapper, PointLogMapper pointLogMapper, SignInMapper signInMapper) {
        this.userMapper = userMapper;
        this.pointLogMapper = pointLogMapper;
        this.signInMapper = signInMapper;
    }

    /** 用户积分画像（含今日签到状态与连续天数） */
    public java.util.Map<String, Object> profile(Long userId) {
        User user = ensureUser(userId);
        // VIP 懒回落：过期即回 FREE
        if ("VIP".equals(user.getLevel()) && user.getVipExpireAt() != null
                && user.getVipExpireAt().isBefore(LocalDateTime.now())) {
            user.setLevel("FREE");
            userMapper.updateById(user);
        }
        LocalDate today = LocalDate.now();
        boolean signedToday = signInMapper.selectCount(new LambdaQueryWrapper<SignIn>()
                .eq(SignIn::getUserId, userId).eq(SignIn::getSignDate, today)) > 0;
        java.util.Map<String, Object> m = new java.util.HashMap<>();
        m.put("points", user.getPoints() == null ? 0 : user.getPoints());
        m.put("level", user.getLevel() == null ? "FREE" : user.getLevel());
        m.put("vipExpireAt", user.getVipExpireAt());
        m.put("signedToday", signedToday);
        m.put("streakDays", calcStreak(userId, today, signedToday));
        return m;
    }

    /** 积分流水（倒序，limit 条） */
    public List<PointLog> logs(Long userId, int limit) {
        return pointLogMapper.selectList(new LambdaQueryWrapper<PointLog>()
                .eq(PointLog::getUserId, userId)
                .orderByDesc(PointLog::getId)
                .last("LIMIT " + Math.min(Math.max(limit, 1), 100)));
    }

    /**
     * 每日签到（幂等）：同日重复签到返回 null 不重复加分。
     * 连续第 7 天（及倍数）额外奖励。
     */
    @Transactional
    public java.util.Map<String, Object> signIn(Long userId) {
        User user = ensureUser(userId);
        LocalDate today = LocalDate.now();
        long todayCount = signInMapper.selectCount(new LambdaQueryWrapper<SignIn>()
                .eq(SignIn::getUserId, userId).eq(SignIn::getSignDate, today));
        if (todayCount > 0) {
            log.info("签到重复: userId={} date={}", userId, today);
            return null;
        }
        int streak = calcStreak(userId, today, false) + 1; // 签到后连续天数
        int points = SIGN_IN_POINTS;
        if (streak % STREAK_CYCLE == 0) {
            points += STREAK_BONUS_POINTS;
        }
        signInMapper.insert(SignIn.builder()
                .userId(userId).signDate(today).points(points)
                .createTime(LocalDateTime.now()).build());
        addPoints(userId, points, "每日签到" + (points > SIGN_IN_POINTS ? "（连续" + streak + "天奖励）" : ""));
        java.util.Map<String, Object> m = new java.util.HashMap<>();
        m.put("points", points);
        m.put("streakDays", streak);
        m.put("bonus", points > SIGN_IN_POINTS);
        return m;
    }

    /** 积分变更（原子）：更新余额 + 写流水；delta 为负数表示扣减 */
    @Transactional
    public void addPoints(Long userId, int delta, String reason) {
        if (delta == 0) return;
        User user = ensureUser(userId);
        int current = user.getPoints() == null ? 0 : user.getPoints();
        int after = Math.max(0, current + delta); // 积分不为负
        user.setPoints(after);
        userMapper.updateById(user);
        pointLogMapper.insert(PointLog.builder()
                .userId(userId).changePoints(delta).reason(reason)
                .createTime(LocalDateTime.now()).build());
        log.info("积分变更: userId={} delta={} reason={} 余额={}", userId, delta, reason, after);
    }

    /** 管理员发放/扣减积分（用户不存在抛业务异常） */
    @Transactional
    public void adminChange(Long userId, int delta, String reason) {
        if (delta == 0) throw new BusinessException("积分变更量不能为 0");
        if (reason == null || reason.isBlank()) throw new BusinessException("请填写积分变更原因");
        addPoints(userId, delta, reason);
    }

    /** 开通/续期 VIP（days 为开通天数，从当前时间起算；已有 VIP 则叠加） */
    @Transactional
    public void grantVip(String username, int days) {
        if (username == null || username.isBlank()) throw new BusinessException("用户名不能为空");
        if (days <= 0) throw new BusinessException("VIP 天数必须大于 0");
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username.trim()));
        if (user == null) throw new BusinessException("用户不存在");
        LocalDateTime base = (user.getVipExpireAt() != null && user.getVipExpireAt().isAfter(LocalDateTime.now()))
                ? user.getVipExpireAt() : LocalDateTime.now();
        user.setVipExpireAt(base.plusDays(days));
        user.setLevel("VIP");
        userMapper.updateById(user);
        log.info("VIP 开通: username={} days={} 到期={}", username, days, user.getVipExpireAt());
    }

    /**
     * 对话消耗积分（商业化闭环：签到赚分 → 聊天花分 → 留存）。
     * VIP / ADMIN 不扣分；FREE 原子扣减（UPDATE ... WHERE points >= cost），
     * 防并发超扣；余额不足抛 BusinessException 由调用方提示。
     */
    public void consumeForChat(String username, int cost) {
        if (username == null || username.isBlank() || cost <= 0) return;
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username.trim()));
        if (user == null) return;
        // VIP / ADMIN 不消耗积分
        if ("ADMIN".equals(user.getRole()) || "VIP".equals(user.getLevel())) return;
        // 原子扣减：单条 SQL 保证并发下不会扣成负数
        int rows = userMapper.update(null, new UpdateWrapper<User>()
                .setSql("points = points - " + cost)
                .eq("id", user.getId())
                .ge("points", cost));
        if (rows == 0) {
            log.info("对话积分不足: username={} cost={}", username, cost);
            throw new BusinessException("积分不足：本次对话需要 " + cost + " 积分，请先到个人中心签到获取");
        }
        pointLogMapper.insert(PointLog.builder()
                .userId(user.getId()).changePoints(-cost).reason("AI 对话消耗")
                .createTime(LocalDateTime.now()).build());
        log.info("对话消耗积分: username={} cost={}", username, cost);
    }

    /** 计算连续签到天数：signedToday 为 true 从今天算起，否则从昨天算起 */
    int calcStreak(Long userId, LocalDate today, boolean signedToday) {
        LocalDate cursor = signedToday ? today : today.minusDays(1);
        int streak = 0;
        while (cursor != null) {
            long count = signInMapper.selectCount(new LambdaQueryWrapper<SignIn>()
                    .eq(SignIn::getUserId, userId).eq(SignIn::getSignDate, cursor));
            if (count == 0) break;
            streak++;
            cursor = cursor.minusDays(1);
        }
        return streak;
    }

    private User ensureUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) throw new BusinessException("用户不存在");
        return user;
    }
}

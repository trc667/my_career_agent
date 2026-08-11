package com.example.aimaster.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.example.aimaster.config.ModelCatalog;
import com.example.aimaster.entity.InviteReward;
import com.example.aimaster.entity.PointLog;
import com.example.aimaster.entity.SignIn;
import com.example.aimaster.entity.User;
import com.example.aimaster.exception.BusinessException;
import com.example.aimaster.mapper.InviteRewardMapper;
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
    /** 邀请好友完成首聊奖励积分 */
    private static final int INVITE_REWARD_POINTS = 50;

    private final UserMapper userMapper;
    private final PointLogMapper pointLogMapper;
    private final SignInMapper signInMapper;
    private final InviteRewardMapper inviteRewardMapper;
    private final ModelCatalog modelCatalog;

    public PointService(UserMapper userMapper, PointLogMapper pointLogMapper, SignInMapper signInMapper,
                        InviteRewardMapper inviteRewardMapper, ModelCatalog modelCatalog) {
        this.userMapper = userMapper;
        this.pointLogMapper = pointLogMapper;
        this.signInMapper = signInMapper;
        this.inviteRewardMapper = inviteRewardMapper;
        this.modelCatalog = modelCatalog;
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
        consumeForChat(username, cost, "AI 对话消耗");
    }

    /** 对话消耗积分（可自定义流水原因，如按模型计费时写 "AI 对话消耗:deepseek-v3"） */
    public void consumeForChat(String username, int cost, String reason) {
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
                .userId(user.getId()).changePoints(-cost)
                .reason(reason != null && !reason.isBlank() ? reason : "AI 对话消耗")
                .createTime(LocalDateTime.now()).build());
        log.info("对话消耗积分: username={} cost={} reason={}", username, cost, reason);
    }

    /**
     * 对话前预检（按 token 计费模式下，调用前只拦截“连最低消费都不够”的用户）：
     * 余额 ≥ 1 分放行（每次对话至少 1 分），VIP/ADMIN 不检。
     */
    public void precheckChat(String username, String model) {
        if (username == null || username.isBlank()) return;
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username.trim()));
        if (user == null) return;
        if ("ADMIN".equals(user.getRole()) || "VIP".equals(user.getLevel())) return;
        int points = user.getPoints() == null ? 0 : user.getPoints();
        if (points < 1) {
            throw new BusinessException("积分不足：" + modelCatalog.nameOf(model) + " 对话至少消耗 1 积分，请先到个人中心签到获取");
        }
    }

    /**
     * 对话结束按实际 token 结算（模型切换计费核心）：
     * cost = max(1, ceil(totalTokens/1000) * 模型费率)；
     * 余额不足时按剩余扣到 0（对话已完成不拦用户，但流水审计完整、不会为负）。
     */
    public void settleChat(String username, String model, int totalTokens) {
        if (username == null || username.isBlank()) return;
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username.trim()));
        if (user == null) return;
        if ("ADMIN".equals(user.getRole()) || "VIP".equals(user.getLevel())) return;
        int rate = modelCatalog.rateOf(model);
        int cost = Math.max(1, (int) Math.ceil((Math.max(totalTokens, 0) / 1000.0)) * rate);
        String reason = "AI 对话消耗:" + modelCatalog.resolve(model);
        int rows = userMapper.update(null, new UpdateWrapper<User>()
                .setSql("points = points - " + cost)
                .eq("id", user.getId())
                .ge("points", cost));
        if (rows == 0) {
            // 余额不足：按剩余积分扣到 0，保证审计完整
            int actual = Math.min(cost, Math.max(0, user.getPoints() == null ? 0 : user.getPoints()));
            if (actual > 0) {
                userMapper.update(null, new UpdateWrapper<User>()
                        .setSql("points = points - " + actual)
                        .eq("id", user.getId()));
                cost = actual;
            } else {
                cost = 0;
            }
        }
        if (cost > 0) {
            pointLogMapper.insert(PointLog.builder()
                    .userId(user.getId()).changePoints(-cost).reason(reason)
                    .createTime(LocalDateTime.now()).build());
        }
        log.info("模型对话结算: username={} model={} tokens={} cost={}", username, model, totalTokens, cost);
    }

    /**
     * 邀请好友完成首轮对话奖励（分享裂变）：被邀人完成首聊后调用。
     * 幂等防刷：invite_reward 表 (inviter_id, invitee_id) 唯一键，同一组合只奖一次；
     * 邀请人可能是 VIP/ADMIN 也正常发分（积分与等级独立）。
     */
    public void rewardInviterOnFirstChat(String inviteeUsername) {
        if (inviteeUsername == null || inviteeUsername.isBlank()) return;
        User invitee = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, inviteeUsername.trim()));
        if (invitee == null || invitee.getInviterId() == null) return;
        Long inviterId = invitee.getInviterId();
        if (userMapper.selectById(inviterId) == null) return; // 邀请人已不存在
        long count = inviteRewardMapper.selectCount(new LambdaQueryWrapper<InviteReward>()
                .eq(InviteReward::getInviterId, inviterId)
                .eq(InviteReward::getInviteeId, invitee.getId()));
        if (count > 0) {
            log.info("邀请奖励已发放过，跳过: inviter={} invitee={}", inviterId, invitee.getId());
            return;
        }
        inviteRewardMapper.insert(InviteReward.builder()
                .inviterId(inviterId).inviteeId(invitee.getId()).points(INVITE_REWARD_POINTS)
                .createTime(LocalDateTime.now()).build());
        addPoints(inviterId, INVITE_REWARD_POINTS, "邀请好友完成首聊奖励");
        log.info("邀请奖励发放: inviter={} invitee={} +{}分", inviterId, invitee.getId(), INVITE_REWARD_POINTS);
    }

    /** 邀请信息（前端「邀请好友」卡片）：邀请码=userId、已成功邀请数、每单奖励 */
    public java.util.Map<String, Object> inviteProfile(Long userId) {
        long rewardedCount = inviteRewardMapper.selectCount(new LambdaQueryWrapper<InviteReward>()
                .eq(InviteReward::getInviterId, userId));
        java.util.Map<String, Object> m = new java.util.HashMap<>();
        m.put("inviteCode", userId);
        m.put("invitedCount", rewardedCount);
        m.put("rewardPoints", INVITE_REWARD_POINTS);
        return m;
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

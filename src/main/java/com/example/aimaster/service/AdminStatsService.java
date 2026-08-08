package com.example.aimaster.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.aimaster.entity.BaguCheckin;
import com.example.aimaster.entity.Conversation;
import com.example.aimaster.entity.InterviewRecord;
import com.example.aimaster.entity.PointLog;
import com.example.aimaster.entity.RedeemRecord;
import com.example.aimaster.entity.SignIn;
import com.example.aimaster.entity.User;
import com.example.aimaster.mapper.BaguCheckinMapper;
import com.example.aimaster.mapper.ConversationMapper;
import com.example.aimaster.mapper.InterviewRecordMapper;
import com.example.aimaster.mapper.PointLogMapper;
import com.example.aimaster.mapper.RedeemRecordMapper;
import com.example.aimaster.mapper.SignInMapper;
import com.example.aimaster.mapper.UserMapper;

import org.springframework.stereotype.Service;

/**
 * 运营看板统计：管理后台了解商业化跑得怎样（用户/活跃/留存/积分/兑换）。
 * <p>
 * 全部只读聚合，口径说明：
 * - 活跃用户 = 今日有对话会话的用户 ∪ 今日签到用户（DISTINCT user_id 合并）；
 * - 留存用「本周新增用户」近似（个人项目未做事件埋点，登录/对话即活跃信号）。
 */
@Service
public class AdminStatsService {

    private final UserMapper userMapper;
    private final ConversationMapper conversationMapper;
    private final SignInMapper signInMapper;
    private final BaguCheckinMapper baguCheckinMapper;
    private final PointLogMapper pointLogMapper;
    private final RedeemRecordMapper redeemRecordMapper;
    private final InterviewRecordMapper interviewRecordMapper;

    public AdminStatsService(UserMapper userMapper, ConversationMapper conversationMapper,
                             SignInMapper signInMapper, BaguCheckinMapper baguCheckinMapper,
                             PointLogMapper pointLogMapper, RedeemRecordMapper redeemRecordMapper,
                             InterviewRecordMapper interviewRecordMapper) {
        this.userMapper = userMapper;
        this.conversationMapper = conversationMapper;
        this.signInMapper = signInMapper;
        this.baguCheckinMapper = baguCheckinMapper;
        this.pointLogMapper = pointLogMapper;
        this.redeemRecordMapper = redeemRecordMapper;
        this.interviewRecordMapper = interviewRecordMapper;
    }

    /** 运营总览统计 */
    public Map<String, Object> overview() {
        LocalDate today = LocalDate.now();
        LocalDateTime weekStart = today.with(DayOfWeek.MONDAY).atStartOfDay();
        LocalDateTime dayStart = today.atStartOfDay();

        Map<String, Object> data = new LinkedHashMap<>();

        // ===== 用户规模 =====
        long totalUsers = userMapper.selectCount(null);
        long newUsersWeek = userMapper.selectCount(new LambdaQueryWrapper<User>()
                .ge(User::getCreateTime, weekStart));
        long vipUsers = userMapper.selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getLevel, "VIP"));
        Map<String, Object> users = new LinkedHashMap<>();
        users.put("total", totalUsers);
        users.put("newWeek", newUsersWeek);
        users.put("vip", vipUsers);
        data.put("users", users);

        // ===== 今日活跃 =====
        List<Object> convActive = conversationMapper.selectObjs(new QueryWrapper<Conversation>()
                .select("DISTINCT user_id").ge("created_at", dayStart));
        List<Object> signActive = signInMapper.selectObjs(new QueryWrapper<SignIn>()
                .select("DISTINCT user_id").ge("sign_date", today));
        java.util.Set<Object> activeSet = new java.util.HashSet<>(convActive);
        activeSet.addAll(signActive);
        data.put("activeToday", activeSet.size());

        // ===== 对话规模 =====
        long totalConvs = conversationMapper.selectCount(null);
        long weekConvs = conversationMapper.selectCount(new LambdaQueryWrapper<Conversation>()
                .ge(Conversation::getCreateTime, weekStart));
        Map<String, Object> convs = new LinkedHashMap<>();
        convs.put("total", totalConvs);
        convs.put("week", weekConvs);
        data.put("conversations", convs);

        // ===== 打卡 =====
        long weekSignDays = signInMapper.selectCount(new LambdaQueryWrapper<SignIn>()
                .ge(SignIn::getSignDate, today.with(DayOfWeek.MONDAY)));
        long weekCheckinDays = baguCheckinMapper.selectCount(new LambdaQueryWrapper<BaguCheckin>()
                .ge(BaguCheckin::getCheckinDate, today.with(DayOfWeek.MONDAY)));
        data.put("weekSignDays", weekSignDays);
        data.put("weekCheckinDays", weekCheckinDays);

        // ===== 积分账本（本周） =====
        List<PointLog> weekLogs = pointLogMapper.selectList(new LambdaQueryWrapper<PointLog>()
                .ge(PointLog::getCreateTime, weekStart));
        int earned = weekLogs.stream().filter(l -> l.getChangePoints() != null && l.getChangePoints() > 0)
                .mapToInt(PointLog::getChangePoints).sum();
        int spent = Math.abs(weekLogs.stream().filter(l -> l.getChangePoints() != null && l.getChangePoints() < 0)
                .mapToInt(PointLog::getChangePoints).sum());
        Map<String, Object> points = new LinkedHashMap<>();
        points.put("earned", earned);
        points.put("spent", spent);
        data.put("points", points);

        // ===== 兑换出口 =====
        List<RedeemRecord> allRedeems = redeemRecordMapper.selectList(null);
        int redeemPoints = allRedeems.stream().mapToInt(r -> r.getPoints() == null ? 0 : r.getPoints()).sum();
        Map<String, Object> redeems = new LinkedHashMap<>();
        redeems.put("count", allRedeems.size());
        redeems.put("points", redeemPoints);
        data.put("redeems", redeems);

        // ===== 面试模拟（VIP 卖点效果） =====
        long totalInterviews = interviewRecordMapper.selectCount(null);
        long weekInterviews = interviewRecordMapper.selectCount(new LambdaQueryWrapper<InterviewRecord>()
                .ge(InterviewRecord::getCreateTime, weekStart));
        Map<String, Object> interviews = new LinkedHashMap<>();
        interviews.put("total", totalInterviews);
        interviews.put("week", weekInterviews);
        data.put("interviews", interviews);

        // ===== 消费去向 Top（本周积分扣减原因 Top 5） =====
        Map<String, Integer> spendByReason = new HashMap<>();
        weekLogs.stream().filter(l -> l.getChangePoints() != null && l.getChangePoints() < 0)
                .forEach(l -> spendByReason.merge(
                        l.getReason() == null || l.getReason().isBlank() ? "其他" : l.getReason(),
                        -l.getChangePoints(), Integer::sum));
        List<Map<String, Object>> spendTop = spendByReason.entrySet().stream()
                .sorted((a, b) -> b.getValue() - a.getValue()).limit(5)
                .map(e -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("reason", e.getKey());
                    m.put("points", e.getValue());
                    return m;
                }).toList();
        data.put("spendTop", spendTop);

        return data;
    }
}

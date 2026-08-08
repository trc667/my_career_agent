package com.example.aimaster.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.aimaster.entity.Conversation;
import com.example.aimaster.entity.PointLog;
import com.example.aimaster.entity.RedeemRecord;
import com.example.aimaster.entity.ResumeReview;
import com.example.aimaster.entity.SignIn;
import com.example.aimaster.entity.BaguCheckin;
import com.example.aimaster.entity.BaguWrong;
import com.example.aimaster.mapper.BaguCheckinMapper;
import com.example.aimaster.mapper.BaguWrongMapper;
import com.example.aimaster.mapper.ConversationMapper;
import com.example.aimaster.mapper.PointLogMapper;
import com.example.aimaster.mapper.RedeemRecordMapper;
import com.example.aimaster.mapper.ResumeReviewMapper;
import com.example.aimaster.mapper.SignInMapper;

import org.springframework.stereotype.Service;

/**
 * 用户学习周报：每周一自动聚合上周（本周一 00:00 起）的学习数据。
 * <p>
 * 数据源全部来自已有落库：对话会话（标题=主题）、签到、八股打卡/错题本、积分流水、兑换记录、简历评分。
 * 纯读聚合零写入，用于留存与数据资产沉淀（面试模拟会话在 Caffeine 缓存不落库，故不纳入周报）。
 */
@Service
public class WeeklyReportService {

    private final ConversationMapper conversationMapper;
    private final SignInMapper signInMapper;
    private final BaguCheckinMapper baguCheckinMapper;
    private final BaguWrongMapper baguWrongMapper;
    private final PointLogMapper pointLogMapper;
    private final RedeemRecordMapper redeemRecordMapper;
    private final ResumeReviewMapper resumeReviewMapper;
    private final AchievementService achievementService;

    public WeeklyReportService(ConversationMapper conversationMapper, SignInMapper signInMapper,
                               BaguCheckinMapper baguCheckinMapper, BaguWrongMapper baguWrongMapper,
                               PointLogMapper pointLogMapper, RedeemRecordMapper redeemRecordMapper,
                               ResumeReviewMapper resumeReviewMapper, AchievementService achievementService) {
        this.conversationMapper = conversationMapper;
        this.signInMapper = signInMapper;
        this.baguCheckinMapper = baguCheckinMapper;
        this.baguWrongMapper = baguWrongMapper;
        this.pointLogMapper = pointLogMapper;
        this.redeemRecordMapper = redeemRecordMapper;
        this.resumeReviewMapper = resumeReviewMapper;
        this.achievementService = achievementService;
    }

    /** 生成本周学习周报（本周一 00:00 起聚合） */
    public Map<String, Object> weekly(Long userId) {
        LocalDate today = LocalDate.now();
        LocalDate monday = today.with(DayOfWeek.MONDAY);
        LocalDateTime start = monday.atStartOfDay();

        Map<String, Object> report = new LinkedHashMap<>();
        report.put("week", monday + " ~ " + today);

        // 对话：本周会话数 + 主题（标题去重取 Top 6）
        List<Conversation> convs = conversationMapper.selectList(new LambdaQueryWrapper<Conversation>()
                .eq(Conversation::getUserId, userId)
                .ge(Conversation::getCreateTime, start));
        Map<String, Object> conv = new LinkedHashMap<>();
        conv.put("count", convs.size());
        conv.put("topics", convs.stream()
                .map(Conversation::getTitle)
                .filter(t -> t != null && !t.isBlank() && !"新的职规咨询".equals(t))
                .distinct().limit(6).toList());
        report.put("conversation", conv);

        // 学习投入：签到天数 / 八股打卡天数 / 错题新增与掌握
        long signDays = signInMapper.selectCount(new LambdaQueryWrapper<SignIn>()
                .eq(SignIn::getUserId, userId).ge(SignIn::getSignDate, monday));
        long checkinDays = baguCheckinMapper.selectCount(new LambdaQueryWrapper<BaguCheckin>()
                .eq(BaguCheckin::getUserId, userId).ge(BaguCheckin::getCheckinDate, monday));
        long newWrong = baguWrongMapper.selectCount(new LambdaQueryWrapper<BaguWrong>()
                .eq(BaguWrong::getUserId, userId).ge(BaguWrong::getCreateTime, start));
        long masteredWrong = baguWrongMapper.selectCount(new LambdaQueryWrapper<BaguWrong>()
                .eq(BaguWrong::getUserId, userId).eq(BaguWrong::getMastered, 1));
        Map<String, Object> learning = new LinkedHashMap<>();
        learning.put("signDays", signDays);
        learning.put("checkinDays", checkinDays);
        learning.put("newWrong", newWrong);
        learning.put("masteredWrong", masteredWrong);
        report.put("learning", learning);

        // 求职产出：简历评分次数
        long resumeReviews = resumeReviewMapper.selectCount(new LambdaQueryWrapper<ResumeReview>()
                .eq(ResumeReview::getUserId, userId).ge(ResumeReview::getCreateTime, start));
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("resumeReviews", resumeReviews);
        report.put("output", output);

        // 积分：本周获得/消耗/净变化/兑换次数
        List<PointLog> logs = pointLogMapper.selectList(new LambdaQueryWrapper<PointLog>()
                .eq(PointLog::getUserId, userId).ge(PointLog::getCreateTime, start));
        int earned = logs.stream().filter(l -> l.getChangePoints() != null && l.getChangePoints() > 0)
                .mapToInt(PointLog::getChangePoints).sum();
        int spent = Math.abs(logs.stream().filter(l -> l.getChangePoints() != null && l.getChangePoints() < 0)
                .mapToInt(PointLog::getChangePoints).sum());
        long redeemCount = redeemRecordMapper.selectCount(new LambdaQueryWrapper<RedeemRecord>()
                .eq(RedeemRecord::getUserId, userId).ge(RedeemRecord::getCreateTime, start));
        Map<String, Object> points = new LinkedHashMap<>();
        points.put("earned", earned);
        points.put("spent", spent);
        points.put("net", earned - spent);
        points.put("redeemCount", redeemCount);
        report.put("points", points);

        // 成就：当前解锁数
        List<AchievementService.Achievement> achvs = achievementService.list(userId);
        Map<String, Object> achv = new LinkedHashMap<>();
        achv.put("unlocked", achvs.stream().filter(AchievementService.Achievement::unlocked).count());
        achv.put("total", achvs.size());
        report.put("achievements", achv);

        report.put("advice", buildAdvice(conv.size(), (long) convs.size(), signDays, newWrong, earned, redeemCount));
        return report;
    }

    /** 规则生成周报建议（不调 LLM，零成本） */
    private String buildAdvice(int topicCount, long convCount, long signDays, long newWrong, int earned, long redeemCount) {
        List<String> tips = new ArrayList<>();
        if (convCount == 0) {
            tips.add("本周还没有和 AI 对话，试着从职业规划或面试准备开始聊一句");
        } else if (topicCount >= 3) {
            tips.add("本周话题很丰富，继续保持多主题探索");
        }
        if (signDays >= 5) {
            tips.add("签到打卡 " + signDays + " 天，自律拉满");
        } else if (signDays > 0) {
            tips.add("本周签到 " + signDays + " 天，再坚持几天可解锁连续奖励");
        }
        if (newWrong > 0) {
            tips.add("错题本新增 " + newWrong + " 题，记得回头复习，掌握后标记\"已掌握\"");
        }
        if (earned >= 50) {
            tips.add("本周赚了 " + earned + " 积分，可以去积分商城兑换资料或 VIP 体验卡");
        } else if (redeemCount == 0 && earned > 0) {
            tips.add("本周赚了 " + earned + " 积分，攒一攒去积分商城看看有什么想要的");
        }
        return tips.isEmpty() ? "本周数据平平，从一次对话或一道题开始积累吧" : String.join("；", tips) + "。";
    }
}

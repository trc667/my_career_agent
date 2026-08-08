package com.example.aimaster.service;

import java.util.ArrayList;
import java.util.List;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.aimaster.entity.InviteReward;
import com.example.aimaster.entity.PointLog;
import com.example.aimaster.entity.SignIn;
import com.example.aimaster.entity.User;
import com.example.aimaster.exception.BusinessException;
import com.example.aimaster.mapper.InviteRewardMapper;
import com.example.aimaster.mapper.PointLogMapper;
import com.example.aimaster.mapper.SignInMapper;
import com.example.aimaster.mapper.UserMapper;

import org.springframework.stereotype.Service;

/**
 * 成就体系：把留存闭环（签到/对话/邀请/积分）游戏化，前端展示徽章墙 + 进度。
 * <p>
 * 设计（面试可讲）：
 * 1) 成就为静态规则 + 运行时数据判定（progress/target），不落表——规则改代码即生效，数据实时算；
 * 2) 数据来源统一复用现有表：sign_in（连续签到）、invite_reward（邀请数）、point_log（对话次数/累计积分）；
 * 3) 返回 progress/target，前端渲染进度条（未解锁置灰），形成"差一点点就解锁"的激励。
 */
@Service
public class AchievementService {

    /** 前端展示的成就条目 */
    public record Achievement(String code, String name, String desc, String icon,
                              long progress, long target, boolean unlocked) {
    }

    private final UserMapper userMapper;
    private final SignInMapper signInMapper;
    private final InviteRewardMapper inviteRewardMapper;
    private final PointLogMapper pointLogMapper;

    public AchievementService(UserMapper userMapper, SignInMapper signInMapper,
                              InviteRewardMapper inviteRewardMapper, PointLogMapper pointLogMapper) {
        this.userMapper = userMapper;
        this.signInMapper = signInMapper;
        this.inviteRewardMapper = inviteRewardMapper;
        this.pointLogMapper = pointLogMapper;
    }

    /** 成就列表（按解锁进度排序：已解锁在前） */
    public List<Achievement> list(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) throw new BusinessException("用户不存在");

        // 连续签到天数（含今天）
        long streak = calcStreak(userId);
        // 成功邀请人数（完成首聊并已奖励）
        long invited = inviteRewardMapper.selectCount(new LambdaQueryWrapper<InviteReward>()
                .eq(InviteReward::getInviterId, userId));
        // AI 对话次数（按对话消耗流水计数，每次对话一条）
        long chats = pointLogMapper.selectCount(new LambdaQueryWrapper<PointLog>()
                .eq(PointLog::getUserId, userId).eq(PointLog::getReason, "AI 对话消耗"));
        // 累计获得积分（正数流水总和）
        long earned = pointLogMapper.selectList(new LambdaQueryWrapper<PointLog>()
                        .eq(PointLog::getUserId, userId).gt(PointLog::getChangePoints, 0))
                .stream().mapToLong(PointLog::getChangePoints).sum();

        List<Achievement> list = new ArrayList<>();
        list.add(build("first_chat", "初次对话", "完成你的第 1 次 AI 咨询", "💬", chats, 1));
        list.add(build("chats_10", "常驻咨询者", "累计完成 10 次对话", "🗨️", chats, 10));
        list.add(build("sign_7", "七天连胜", "连续签到 7 天", "🔥", streak, 7));
        list.add(build("sign_30", "月度铁人", "连续签到 30 天", "🏆", streak, 30));
        list.add(build("invite_1", "初次引荐", "成功邀请 1 位好友", "🤝", invited, 1));
        list.add(build("invite_5", "人气星主", "成功邀请 5 位好友", "🌟", invited, 5));
        list.add(build("invite_10", "社交达人", "成功邀请 10 位好友", "👑", invited, 10));
        list.add(build("points_100", "百积分户", "累计获得 100 积分", "💰", earned, 100));
        list.add(build("points_500", "积分富翁", "累计获得 500 积分", "💎", earned, 500));

        list.sort((a, b) -> Boolean.compare(b.unlocked(), a.unlocked()));
        return list;
    }

    private Achievement build(String code, String name, String desc, String icon,
                              long progress, long target) {
        return new Achievement(code, name, desc, icon, Math.min(progress, target), target, progress >= target);
    }

    /** 连续签到天数（今天已签则含今天，否则从昨天往前数） */
    private long calcStreak(Long userId) {
        long streak = 0;
        java.time.LocalDate cursor = java.time.LocalDate.now();
        // 今天未签则从昨天开始数
        long today = signInMapper.selectCount(new LambdaQueryWrapper<SignIn>()
                .eq(SignIn::getUserId, userId).eq(SignIn::getSignDate, cursor));
        if (today == 0) cursor = cursor.minusDays(1);
        while (true) {
            long count = signInMapper.selectCount(new LambdaQueryWrapper<SignIn>()
                    .eq(SignIn::getUserId, userId).eq(SignIn::getSignDate, cursor));
            if (count == 0) break;
            streak++;
            cursor = cursor.minusDays(1);
        }
        return streak;
    }
}

package com.example.aimaster.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.aimaster.entity.Conversation;
import com.example.aimaster.entity.InterviewRecord;
import com.example.aimaster.entity.RedeemRecord;
import com.example.aimaster.entity.SignIn;
import com.example.aimaster.entity.UserTask;
import com.example.aimaster.exception.BusinessException;
import com.example.aimaster.mapper.ConversationMapper;
import com.example.aimaster.mapper.InterviewRecordMapper;
import com.example.aimaster.mapper.RedeemRecordMapper;
import com.example.aimaster.mapper.SignInMapper;
import com.example.aimaster.mapper.UserTaskMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * 新手引导任务（商业留存闭环：新用户完成关键动作 → 得积分 → 进入签到/消费循环）。
 * 完成状态从业务表实时判定（无冗余字段），领取记录落 user_task（唯一约束幂等防刷）。
 */
@Slf4j
@Service
public class GuideTaskService {

    /** 任务定义 */
    public record TaskDef(String key, String name, String desc, int rewardPoints) {
    }

    private final ConversationMapper conversationMapper;
    private final SignInMapper signInMapper;
    private final InterviewRecordMapper interviewRecordMapper;
    private final RedeemRecordMapper redeemRecordMapper;
    private final UserTaskMapper userTaskMapper;
    private final PointService pointService;

    public GuideTaskService(ConversationMapper conversationMapper,
                            SignInMapper signInMapper,
                            InterviewRecordMapper interviewRecordMapper,
                            RedeemRecordMapper redeemRecordMapper,
                            UserTaskMapper userTaskMapper,
                            PointService pointService) {
        this.conversationMapper = conversationMapper;
        this.signInMapper = signInMapper;
        this.interviewRecordMapper = interviewRecordMapper;
        this.redeemRecordMapper = redeemRecordMapper;
        this.userTaskMapper = userTaskMapper;
        this.pointService = pointService;
    }

    /** 4 个新手任务：完成关键动作给积分，把新用户推入留存闭环（共 40 分 ≈ 2-3 次对话成本） */
    private final List<TaskDef> TASKS = List.of(
            new TaskDef("first_chat", "首次对话", "和职规大师聊一次，开启求职咨询", 10),
            new TaskDef("first_sign", "首次签到", "到个人中心签到，每天都有积分", 5),
            new TaskDef("first_interview", "首次面试", "完成一场 AI 模拟面试", 15),
            new TaskDef("first_redeem", "首次兑换", "在积分商城兑换第一件好物", 10));

    /** 任务完成判定：查各业务表是否有记录 */
    private boolean isDone(String key, Long userId) {
        return switch (key) {
            case "first_chat" -> conversationMapper.selectCount(
                    new LambdaQueryWrapper<Conversation>().eq(Conversation::getUserId, userId)) > 0;
            case "first_sign" -> signInMapper.selectCount(
                    new LambdaQueryWrapper<SignIn>().eq(SignIn::getUserId, userId)) > 0;
            case "first_interview" -> interviewRecordMapper.selectCount(
                    new LambdaQueryWrapper<InterviewRecord>().eq(InterviewRecord::getUserId, userId)) > 0;
            case "first_redeem" -> redeemRecordMapper.selectCount(
                    new LambdaQueryWrapper<RedeemRecord>().eq(RedeemRecord::getUserId, userId)) > 0;
            default -> false;
        };
    }

    /** 是否已领取奖励 */
    private boolean isClaimed(Long userId, String key) {
        return userTaskMapper.selectCount(new LambdaQueryWrapper<UserTask>()
                .eq(UserTask::getUserId, userId).eq(UserTask::getTaskKey, key)) > 0;
    }

    /**
     * 任务列表：{key, name, desc, rewardPoints, done, claimed, canClaim}。
     * canClaim = 已完成 && 未领取。
     */
    public List<Map<String, Object>> list(Long userId) {
        List<Map<String, Object>> data = new ArrayList<>(TASKS.size());
        for (TaskDef t : TASKS) {
            boolean done = isDone(t.key(), userId);
            boolean claimed = isClaimed(userId, t.key());
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("key", t.key());
            item.put("name", t.name());
            item.put("desc", t.desc());
            item.put("rewardPoints", t.rewardPoints());
            item.put("done", done);
            item.put("claimed", claimed);
            item.put("canClaim", done && !claimed);
            data.add(item);
        }
        return data;
    }

    /**
     * 领取任务奖励：未完成/已领取抛业务异常；并发重复领取由唯一约束兜底（幂等）。
     *
     * @return 领取的积分数
     */
    public int claim(Long userId, String key) {
        TaskDef task = TASKS.stream().filter(t -> t.key().equals(key)).findFirst()
                .orElseThrow(() -> new BusinessException("任务不存在"));
        if (!isDone(key, userId)) {
            throw new BusinessException("任务尚未完成：" + task.name());
        }
        if (isClaimed(userId, key)) {
            throw new BusinessException("该任务奖励已领取");
        }
        try {
            userTaskMapper.insert(UserTask.builder()
                    .userId(userId).taskKey(key).rewardPoints(task.rewardPoints())
                    .createTime(LocalDateTime.now()).build());
        } catch (DuplicateKeyException e) {
            // 并发重复领取：唯一约束拦截，视为已领取
            log.info("新手任务重复领取拦截: userId={} key={}", userId, key);
            throw new BusinessException("该任务奖励已领取");
        }
        pointService.addPoints(userId, task.rewardPoints(), "新手任务:" + task.name());
        log.info("新手任务奖励发放: userId={} key={} points={}", userId, key, task.rewardPoints());
        return task.rewardPoints();
    }
}

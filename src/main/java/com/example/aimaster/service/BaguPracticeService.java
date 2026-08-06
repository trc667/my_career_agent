package com.example.aimaster.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.aimaster.entity.BaguCheckin;
import com.example.aimaster.entity.BaguWrong;
import com.example.aimaster.exception.BusinessException;
import com.example.aimaster.mapper.BaguCheckinMapper;
import com.example.aimaster.mapper.BaguWrongMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 八股练习配套服务：错题本（加入/重错累计/标记掌握/删除）+ 每日打卡（连续/累计天数）+ 学习统计。
 * <p>
 * 设计（面试可讲）：
 * 1) 错题以内容 hash（question_id）去重，同一用户同一题唯一，重复答错累计次数而非插入多行；
 * 2) 打卡表同用户同日期唯一索引兜底幂等，重复打卡返回当前连续天数不报错；
 * 3) streak 连续天数仅查表回溯，不依赖第三方日历服务。
 */
@Service
public class BaguPracticeService {

    private final BaguWrongMapper wrongMapper;
    private final BaguCheckinMapper checkinMapper;

    public BaguPracticeService(BaguWrongMapper wrongMapper, BaguCheckinMapper checkinMapper) {
        this.wrongMapper = wrongMapper;
        this.checkinMapper = checkinMapper;
    }

    /* ===== 错题本 ===== */

    /** 加入/更新错题：同一用户同一题存在则 wrong_count+1 并重置 mastered */
    public BaguWrong addWrong(Long userId, String questionId, String category, String content) {
        BaguWrong existing = findWrongByQuestion(userId, questionId);
        if (existing != null) {
            existing.setWrongCount((existing.getWrongCount() == null ? 0 : existing.getWrongCount()) + 1);
            existing.setMastered(0);
            existing.setLastWrongAt(LocalDateTime.now());
            wrongMapper.updateById(existing);
            return existing;
        }
        BaguWrong row = BaguWrong.builder()
                .userId(userId)
                .questionId(questionId)
                .questionContent(content)
                .category(category == null ? "" : category)
                .wrongCount(1)
                .lastWrongAt(LocalDateTime.now())
                .mastered(0)
                .createTime(LocalDateTime.now())
                .build();
        wrongMapper.insert(row);
        return row;
    }

    /** 错题列表（未掌握，按最近答错时间倒序） */
    public List<BaguWrong> listWrong(Long userId) {
        return wrongMapper.selectList(new LambdaQueryWrapper<BaguWrong>()
                .eq(BaguWrong::getUserId, userId)
                .eq(BaguWrong::getMastered, 0)
                .orderByDesc(BaguWrong::getLastWrongAt));
    }

    /** 标记掌握（从错题列表隐藏） */
    public void markMastered(Long userId, Long wrongId) {
        BaguWrong row = findOwned(userId, wrongId);
        if (row == null) throw new BusinessException(404, "错题不存在");
        row.setMastered(1);
        wrongMapper.updateById(row);
    }

    /** 删除错题 */
    public void deleteWrong(Long userId, Long wrongId) {
        BaguWrong row = findOwned(userId, wrongId);
        if (row == null) throw new BusinessException(404, "错题不存在");
        wrongMapper.deleteById(row.getId());
    }

    /* ===== 每日打卡 ===== */

    /** 今日打卡（幂等：当天已打卡直接返回当前连续天数） */
    public Map<String, Object> checkin(Long userId) {
        LocalDate today = LocalDate.now();
        boolean todayChecked = existsCheckin(userId, today);
        if (!todayChecked) {
            checkinMapper.insert(BaguCheckin.builder()
                    .userId(userId)
                    .checkinDate(today)
                    .createTime(LocalDateTime.now())
                    .build());
        }
        int streak = calcStreak(userId, today);
        return statusMap(true, streak, totalDays(userId));
    }

    /** 打卡状态：今日是否已打、连续天数（今天未打则从昨天起算）、累计天数 */
    public Map<String, Object> checkinStatus(Long userId) {
        LocalDate today = LocalDate.now();
        boolean todayChecked = existsCheckin(userId, today);
        int streak = todayChecked
                ? calcStreak(userId, today)
                : calcStreak(userId, today.minusDays(1));
        return statusMap(todayChecked, streak, totalDays(userId));
    }

    /* ===== 学习统计 ===== */

    public Map<String, Object> stats(Long userId) {
        Map<String, Object> stats = new HashMap<>();
        Long totalWrong = wrongMapper.selectCount(new LambdaQueryWrapper<BaguWrong>()
                .eq(BaguWrong::getUserId, userId));
        Long mastered = wrongMapper.selectCount(new LambdaQueryWrapper<BaguWrong>()
                .eq(BaguWrong::getUserId, userId)
                .eq(BaguWrong::getMastered, 1));
        stats.put("totalWrong", totalWrong == null ? 0 : totalWrong);
        stats.put("masteredCount", mastered == null ? 0 : mastered);
        stats.put("activeWrong", Math.max(0, (totalWrong == null ? 0 : totalWrong) - (mastered == null ? 0 : mastered)));
        LocalDate today = LocalDate.now();
        int streak = existsCheckin(userId, today)
                ? calcStreak(userId, today)
                : calcStreak(userId, today.minusDays(1));
        stats.put("streak", streak);
        stats.put("totalDays", totalDays(userId));
        return stats;
    }

    /* ===== 私有工具 ===== */

    private BaguWrong findWrongByQuestion(Long userId, String questionId) {
        if (userId == null || questionId == null || questionId.isBlank()) return null;
        return wrongMapper.selectOne(new LambdaQueryWrapper<BaguWrong>()
                .eq(BaguWrong::getUserId, userId)
                .eq(BaguWrong::getQuestionId, questionId));
    }

    private BaguWrong findOwned(Long userId, Long wrongId) {
        if (userId == null || wrongId == null) return null;
        return wrongMapper.selectOne(new LambdaQueryWrapper<BaguWrong>()
                .eq(BaguWrong::getUserId, userId)
                .eq(BaguWrong::getId, wrongId));
    }

    private boolean existsCheckin(Long userId, LocalDate date) {
        Long count = checkinMapper.selectCount(new LambdaQueryWrapper<BaguCheckin>()
                .eq(BaguCheckin::getUserId, userId)
                .eq(BaguCheckin::getCheckinDate, date));
        return count != null && count > 0;
    }

    /** 从某天起向前连续存在打卡的天数 */
    private int calcStreak(Long userId, LocalDate start) {
        int streak = 0;
        LocalDate d = start;
        while (existsCheckin(userId, d)) {
            streak++;
            d = d.minusDays(1);
        }
        return streak;
    }

    private long totalDays(Long userId) {
        Long count = checkinMapper.selectCount(new LambdaQueryWrapper<BaguCheckin>()
                .eq(BaguCheckin::getUserId, userId));
        return count == null ? 0 : count;
    }

    private Map<String, Object> statusMap(boolean todayChecked, int streak, long totalDays) {
        Map<String, Object> m = new HashMap<>();
        m.put("todayChecked", todayChecked);
        m.put("streak", streak);
        m.put("totalDays", totalDays);
        return m;
    }
}

package com.example.aimaster.controller;

import com.example.aimaster.dto.AddWrongRequest;
import com.example.aimaster.dto.Result;
import com.example.aimaster.entity.BaguWrong;
import com.example.aimaster.entity.User;
import com.example.aimaster.service.AuthService;
import com.example.aimaster.service.BaguPracticeService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 八股练习配套接口（均需登录，数据按当前用户隔离）：
 * 错题本（加入/列表/标记掌握/删除）+ 每日打卡（幂等）+ 学习统计。
 */
@RestController
@RequestMapping("/api/bagu/practice")
public class BaguPracticeController {

    private final AuthService authService;
    private final BaguPracticeService practiceService;

    public BaguPracticeController(AuthService authService, BaguPracticeService practiceService) {
        this.authService = authService;
        this.practiceService = practiceService;
    }

    /** 从 JWT 上下文解析当前用户 ID（未登录返回 null） */
    private Long currentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return null;
        User user = authService.getUserInfo(auth.getName());
        return user != null ? user.getId() : null;
    }

    /** POST /api/bagu/practice/wrong 加入/更新错题 */
    @PostMapping("/wrong")
    public Result<BaguWrong> addWrong(@Valid @RequestBody AddWrongRequest req) {
        Long userId = currentUserId();
        if (userId == null) return Result.fail(401, "未登录或账号不存在");
        return Result.ok(practiceService.addWrong(userId, req.getQuestionId(), req.getCategory(), req.getContent()));
    }

    /** GET /api/bagu/practice/wrong 错题列表（未掌握，按最近答错倒序） */
    @GetMapping("/wrong")
    public Result<List<BaguWrong>> listWrong() {
        Long userId = currentUserId();
        if (userId == null) return Result.fail(401, "未登录或账号不存在");
        return Result.ok(practiceService.listWrong(userId));
    }

    /** PUT /api/bagu/practice/wrong/{id}/mastered 标记掌握 */
    @PutMapping("/wrong/{id}/mastered")
    public Result<Void> markMastered(@PathVariable Long id) {
        Long userId = currentUserId();
        if (userId == null) return Result.fail(401, "未登录或账号不存在");
        practiceService.markMastered(userId, id);
        return Result.ok("已标记掌握", null);
    }

    /** DELETE /api/bagu/practice/wrong/{id} 删除错题 */
    @DeleteMapping("/wrong/{id}")
    public Result<Void> deleteWrong(@PathVariable Long id) {
        Long userId = currentUserId();
        if (userId == null) return Result.fail(401, "未登录或账号不存在");
        practiceService.deleteWrong(userId, id);
        return Result.ok("已删除", null);
    }

    /** POST /api/bagu/practice/checkin 今日打卡（幂等） */
    @PostMapping("/checkin")
    public Result<Map<String, Object>> checkin() {
        Long userId = currentUserId();
        if (userId == null) return Result.fail(401, "未登录或账号不存在");
        return Result.ok(practiceService.checkin(userId));
    }

    /** GET /api/bagu/practice/checkin/status 打卡状态 */
    @GetMapping("/checkin/status")
    public Result<Map<String, Object>> checkinStatus() {
        Long userId = currentUserId();
        if (userId == null) return Result.fail(401, "未登录或账号不存在");
        return Result.ok(practiceService.checkinStatus(userId));
    }

    /** GET /api/bagu/practice/stats 学习统计 */
    @GetMapping("/stats")
    public Result<Map<String, Object>> stats() {
        Long userId = currentUserId();
        if (userId == null) return Result.fail(401, "未登录或账号不存在");
        return Result.ok(practiceService.stats(userId));
    }
}

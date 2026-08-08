package com.example.aimaster.controller;

import java.util.Map;

import com.example.aimaster.dto.InterviewAnswerRequest;
import com.example.aimaster.dto.InterviewStartRequest;
import com.example.aimaster.dto.Result;
import com.example.aimaster.entity.User;
import com.example.aimaster.service.AuthService;
import com.example.aimaster.service.InterviewService;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

/**
 * AI 面试模拟接口（均需登录）。
 * <p>
 * 流程：start 开始（选岗位出题）→ answer 逐题作答（AI 点评）→ report 总结报告。
 * FREE 每日 2 次，VIP 不限次（InterviewService 判定）。
 */
@RestController
@RequestMapping("/api/interview")
public class InterviewController {

    private final InterviewService interviewService;
    private final AuthService authService;

    public InterviewController(InterviewService interviewService, AuthService authService) {
        this.interviewService = interviewService;
        this.authService = authService;
    }

    private Long currentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return null;
        User user = authService.getUserInfo(auth.getName());
        return user != null ? user.getId() : null;
    }

    /** POST /api/interview/start 开始面试（选岗位，返回第 1 题） */
    @PostMapping("/start")
    public Result<Map<String, Object>> start(@Valid @RequestBody InterviewStartRequest req) {
        Long userId = currentUserId();
        if (userId == null) return Result.fail(401, "未登录或账号不存在");
        return Result.ok(interviewService.start(userId, req));
    }

    /** POST /api/interview/answer 作答当前题（返回 AI 点评 + 下一题/结束） */
    @PostMapping("/answer")
    public Result<Map<String, Object>> answer(@Valid @RequestBody InterviewAnswerRequest req) {
        Long userId = currentUserId();
        if (userId == null) return Result.fail(401, "未登录或账号不存在");
        return Result.ok(interviewService.answer(userId, req));
    }

    /** GET /api/interview/report 面试总结报告（总分/分维度均值/题目明细） */
    @GetMapping("/report")
    public Result<Map<String, Object>> report(@RequestParam String sessionId) {
        Long userId = currentUserId();
        if (userId == null) return Result.fail(401, "未登录或账号不存在");
        return Result.ok(interviewService.report(userId, sessionId));
    }

    /** GET /api/interview/quota 今日剩余面试次数（VIP 返回 -1 不限） */
    @GetMapping("/quota")
    public Result<Map<String, Object>> quota() {
        Long userId = currentUserId();
        if (userId == null) return Result.fail(401, "未登录或账号不存在");
        return Result.ok(interviewService.quota(userId));
    }

    /** GET /api/interview/records 我的面试记录列表（完成即落库） */
    @GetMapping("/records")
    public Result<java.util.List<Map<String, Object>>> records() {
        Long userId = currentUserId();
        if (userId == null) return Result.fail(401, "未登录或账号不存在");
        return Result.ok(interviewService.records(userId));
    }

    /** GET /api/interview/records/{id} 单场面试详情（含逐题明细） */
    @GetMapping("/records/{id}")
    public Result<Map<String, Object>> recordDetail(@PathVariable Long id) {
        Long userId = currentUserId();
        if (userId == null) return Result.fail(401, "未登录或账号不存在");
        return Result.ok(interviewService.recordDetail(userId, id));
    }

    /** POST /api/interview/records/{id}/wrong?index=0 将某题加入错题本（幂等） */
    @PostMapping("/records/{id}/wrong")
    public Result<Map<String, Object>> addWrong(@PathVariable Long id, @RequestParam int index) {
        Long userId = currentUserId();
        if (userId == null) return Result.fail(401, "未登录或账号不存在");
        return Result.ok(interviewService.addToWrong(userId, id, index));
    }
}

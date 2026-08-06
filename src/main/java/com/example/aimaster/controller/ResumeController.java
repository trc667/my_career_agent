package com.example.aimaster.controller;

import com.example.aimaster.dto.ResumeReviewRequest;
import com.example.aimaster.dto.ResumeReviewResult;
import com.example.aimaster.dto.Result;
import com.example.aimaster.entity.User;
import com.example.aimaster.service.AuthService;
import com.example.aimaster.service.ResumeReviewService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 简历评分接口（均需登录，数据按当前用户隔离）。
 * <ul>
 *   <li>POST /review：评分并保存</li>
 *   <li>GET /reviews：历史概要列表</li>
 *   <li>GET /reviews/{id}：评分详情</li>
 *   <li>DELETE /reviews/{id}：删除记录</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/resume")
public class ResumeController {

    private final AuthService authService;
    private final ResumeReviewService resumeReviewService;

    public ResumeController(AuthService authService, ResumeReviewService resumeReviewService) {
        this.authService = authService;
        this.resumeReviewService = resumeReviewService;
    }

    /** 从 JWT 上下文解析当前用户 ID（未登录返回 null） */
    private Long currentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return null;
        User user = authService.getUserInfo(auth.getName());
        return user != null ? user.getId() : null;
    }

    /** POST /api/resume/review 评分并保存 */
    @PostMapping("/review")
    public Result<ResumeReviewResult> review(@Valid @RequestBody ResumeReviewRequest req) {
        Long userId = currentUserId();
        if (userId == null) return Result.fail(401, "未登录或账号不存在");
        return Result.ok(resumeReviewService.review(userId, req));
    }

    /** GET /api/resume/reviews 历史概要列表 */
    @GetMapping("/reviews")
    public Result<List<Map<String, Object>>> list() {
        Long userId = currentUserId();
        if (userId == null) return Result.fail(401, "未登录或账号不存在");
        return Result.ok(resumeReviewService.list(userId));
    }

    /** GET /api/resume/reviews/{id} 评分详情 */
    @GetMapping("/reviews/{id}")
    public Result<Map<String, Object>> detail(@PathVariable Long id) {
        Long userId = currentUserId();
        if (userId == null) return Result.fail(401, "未登录或账号不存在");
        return Result.ok(resumeReviewService.detail(userId, id));
    }

    /** DELETE /api/resume/reviews/{id} 删除评分记录 */
    @DeleteMapping("/reviews/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        Long userId = currentUserId();
        if (userId == null) return Result.fail(401, "未登录或账号不存在");
        resumeReviewService.delete(userId, id);
        return Result.ok("删除成功", null);
    }
}

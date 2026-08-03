package com.example.aimaster.controller;

import com.example.aimaster.dto.ChangePasswordRequest;
import com.example.aimaster.dto.FeedbackRequest;
import com.example.aimaster.dto.Result;
import com.example.aimaster.entity.Feedback;
import com.example.aimaster.entity.User;
import com.example.aimaster.mapper.FeedbackMapper;
import com.example.aimaster.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 个人中心 + 意见反馈接口（均需登录，用户名从 JWT 解析）。
 */
@RestController
@RequestMapping("/api/user")
public class UserController {

    private final AuthService authService;
    private final FeedbackMapper feedbackMapper;

    public UserController(AuthService authService, FeedbackMapper feedbackMapper) {
        this.authService = authService;
        this.feedbackMapper = feedbackMapper;
    }

    /** 获取当前登录用户名（从 SecurityContext 取，JWT 过滤器已注入） */
    private String currentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : "";
    }

    /** GET /api/user/me 当前用户信息（不含密码哈希） */
    @GetMapping("/me")
    public Result<Map<String, Object>> me() {
        User user = authService.getUserInfo(currentUsername());
        Map<String, Object> info = new HashMap<>();
        info.put("id", user.getId());
        info.put("username", user.getUsername());
        info.put("createTime", user.getCreateTime());
        return Result.ok(info);
    }

    /** POST /api/user/change-password 修改密码 */
    @PostMapping("/change-password")
    public Result<Void> changePassword(@Valid @RequestBody ChangePasswordRequest req) {
        authService.changePassword(currentUsername(), req.getOldPassword(), req.getNewPassword());
        return Result.ok("密码修改成功", null);
    }

    /** POST /api/user/feedback 提交意见反馈 */
    @PostMapping("/feedback")
    public Result<Void> feedback(@Valid @RequestBody FeedbackRequest req) {
        Feedback f = Feedback.builder()
                .username(currentUsername())
                .contact(req.getContact() != null ? req.getContact().trim() : "")
                .content(req.getContent().trim())
                .createTime(LocalDateTime.now())
                .build();
        feedbackMapper.insert(f);
        return Result.ok("反馈提交成功，感谢您的建议！", null);
    }
}

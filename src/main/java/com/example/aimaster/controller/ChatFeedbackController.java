package com.example.aimaster.controller;

import com.example.aimaster.dto.ChatFeedbackRequest;
import com.example.aimaster.dto.Result;
import com.example.aimaster.entity.User;
import com.example.aimaster.service.AuthService;
import com.example.aimaster.service.ChatFeedbackService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 问答反馈接口：点赞/点踩一条 AI 回复（需登录）。
 */
@RestController
@RequestMapping("/api/chat")
public class ChatFeedbackController {

    private final AuthService authService;
    private final ChatFeedbackService feedbackService;

    public ChatFeedbackController(AuthService authService, ChatFeedbackService feedbackService) {
        this.authService = authService;
        this.feedbackService = feedbackService;
    }

    /** POST /api/chat/feedback 提交/切换问答反馈 */
    @PostMapping("/feedback")
    public Result<Void> feedback(@Valid @RequestBody ChatFeedbackRequest req) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = auth != null ? authService.getUserInfo(auth.getName()) : null;
        if (user == null) return Result.fail(401, "未登录或账号不存在");
        feedbackService.save(user.getId(), req);
        return Result.ok("已记录", null);
    }
}

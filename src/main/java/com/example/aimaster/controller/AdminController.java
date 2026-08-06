package com.example.aimaster.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.aimaster.dto.AnnouncementRequest;
import com.example.aimaster.dto.Result;
import com.example.aimaster.entity.Announcement;
import com.example.aimaster.entity.Feedback;
import com.example.aimaster.entity.User;
import com.example.aimaster.exception.BusinessException;
import com.example.aimaster.mapper.AnnouncementMapper;
import com.example.aimaster.mapper.FeedbackMapper;
import com.example.aimaster.mapper.UserMapper;
import com.example.aimaster.service.OssStorageService;
import com.example.aimaster.service.ErrorLogService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 管理后台接口：公告管理、意见反馈查看、用户列表。
 * 全部要求 ROLE_ADMIN（由 SecurityConfig 统一拦截）。
 */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AnnouncementMapper announcementMapper;
    private final FeedbackMapper feedbackMapper;
    private final UserMapper userMapper;
    private final OssStorageService ossStorageService;
    private final ErrorLogService errorLogService;

    public AdminController(AnnouncementMapper announcementMapper,
                           FeedbackMapper feedbackMapper,
                           UserMapper userMapper,
                           OssStorageService ossStorageService,
                           ErrorLogService errorLogService) {
        this.announcementMapper = announcementMapper;
        this.feedbackMapper = feedbackMapper;
        this.userMapper = userMapper;
        this.ossStorageService = ossStorageService;
        this.errorLogService = errorLogService;
    }

    /* ===== 公告管理 ===== */

    @GetMapping("/announcements")
    public Result<List<Announcement>> announcements() {
        return Result.ok(announcementMapper.selectList(
                new LambdaQueryWrapper<Announcement>().orderByDesc(Announcement::getId)
        ));
    }

    @PostMapping("/announcements")
    public Result<Void> createAnnouncement(@Valid @RequestBody AnnouncementRequest req) {
        announcementMapper.insert(Announcement.builder()
                .title(req.getTitle().trim())
                .content(req.getContent().trim())
                .createTime(LocalDateTime.now())
                .build());
        return Result.ok("公告已发布", null);
    }

    @PutMapping("/announcements/{id}")
    public Result<Void> updateAnnouncement(@PathVariable Long id,
                                           @Valid @RequestBody AnnouncementRequest req) {
        Announcement ann = announcementMapper.selectById(id);
        if (ann == null) {
            throw new BusinessException("公告不存在");
        }
        ann.setTitle(req.getTitle().trim());
        ann.setContent(req.getContent().trim());
        announcementMapper.updateById(ann);
        return Result.ok("公告已更新", null);
    }

    @DeleteMapping("/announcements/{id}")
    public Result<Void> deleteAnnouncement(@PathVariable Long id) {
        announcementMapper.deleteById(id);
        return Result.ok("公告已删除", null);
    }

    /* ===== 意见反馈 ===== */

    @GetMapping("/feedbacks")
    public Result<List<Feedback>> feedbacks() {
        return Result.ok(feedbackMapper.selectList(
                new LambdaQueryWrapper<Feedback>().orderByDesc(Feedback::getId)
        ));
    }

    @DeleteMapping("/feedbacks/{id}")
    public Result<Void> deleteFeedback(@PathVariable Long id) {
        feedbackMapper.deleteById(id);
        return Result.ok("反馈已删除", null);
    }

    /* ===== 用户列表 ===== */

    @GetMapping("/users")
    public Result<List<Map<String, Object>>> users() {
        List<User> users = userMapper.selectList(
                new LambdaQueryWrapper<User>().orderByAsc(User::getId)
        );
        List<Map<String, Object>> result = users.stream().map(u -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", u.getId());
            m.put("username", u.getUsername());
            m.put("email", u.getEmail());
            m.put("role", u.getRole() != null ? u.getRole() : "USER");
            m.put("createTime", u.getCreateTime());
            return m;
        }).toList();
        return Result.ok(result);
    }

    /* ===== AI 头像（全局，所有人可见） ===== */

    /** POST /api/admin/ai-avatar 上传/更换 AI 头像（覆盖固定 key，全体用户可见） */
    @PostMapping("/ai-avatar")
    public Result<Map<String, Object>> uploadAiAvatar(@RequestParam("file") MultipartFile file) {
        String url = ossStorageService.uploadAiAvatar(file);
        Map<String, Object> data = new HashMap<>();
        data.put("avatar", url);
        return Result.ok(data);
    }

    /* ===== 错误日志（自建监控面板） ===== */

    /** GET /api/admin/error-logs 错误日志列表（倒序，可按来源/级别过滤） */
    @GetMapping("/error-logs")
    public Result<List<com.example.aimaster.entity.ErrorLog>> errorLogs(
            @RequestParam(required = false) String source,
            @RequestParam(required = false) String level,
            @RequestParam(required = false, defaultValue = "100") int limit) {
        return Result.ok(errorLogService.list(source, level, limit));
    }

    /** DELETE /api/admin/error-logs 清空错误日志 */
    @DeleteMapping("/error-logs")
    public Result<Void> clearErrorLogs() {
        errorLogService.clear();
        return Result.ok("已清空", null);
    }
}

package com.example.aimaster.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.aimaster.dto.ChangePasswordRequest;
import com.example.aimaster.dto.FeedbackRequest;
import com.example.aimaster.dto.Result;
import com.example.aimaster.entity.Feedback;
import com.example.aimaster.entity.User;
import com.example.aimaster.mapper.FeedbackMapper;
import com.example.aimaster.mapper.UserMapper;
import com.example.aimaster.service.AuthService;
import com.example.aimaster.service.OssStorageService;
import com.example.aimaster.service.PointService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

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
    private final OssStorageService ossStorageService;
    private final UserMapper userMapper;
    private final PointService pointService;

    public UserController(AuthService authService,
                          FeedbackMapper feedbackMapper,
                          OssStorageService ossStorageService,
                          UserMapper userMapper,
                          PointService pointService) {
        this.authService = authService;
        this.feedbackMapper = feedbackMapper;
        this.ossStorageService = ossStorageService;
        this.userMapper = userMapper;
        this.pointService = pointService;
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
        info.put("avatar", user.getAvatar());
        return Result.ok(info);
    }

    /** POST /api/user/avatar 上传/更换个人头像（multipart/form-data 字段名 file） */
    @PostMapping("/avatar")
    public Result<Map<String, Object>> uploadAvatar(@RequestParam("file") MultipartFile file) {
        String url = ossStorageService.uploadUserAvatar(file);
        User user = authService.getUserInfo(currentUsername());
        user.setAvatar(url);
        userMapper.update(user,
                new LambdaQueryWrapper<User>().eq(User::getUsername, currentUsername()));
        Map<String, Object> data = new HashMap<>();
        data.put("avatar", url);
        return Result.ok(data);
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

    /* ===== 积分/会员（商业化） ===== */

    /** 当前用户 ID（未登录返回 null） */
    private Long currentUserId() {
        User user = authService.getUserInfo(currentUsername());
        return user != null ? user.getId() : null;
    }

    /** GET /api/user/points 积分画像（余额/等级/到期/今日签到/连续天数/最近流水） */
    @GetMapping("/points")
    public Result<Map<String, Object>> points() {
        Long userId = currentUserId();
        if (userId == null) return Result.fail(401, "未登录或账号不存在");
        Map<String, Object> data = pointService.profile(userId);
        data.put("logs", pointService.logs(userId, 20));
        return Result.ok(data);
    }

    /** POST /api/user/sign-in 每日签到（幂等：同日重复签到返回已签到） */
    @PostMapping("/sign-in")
    public Result<Map<String, Object>> signIn() {
        Long userId = currentUserId();
        if (userId == null) return Result.fail(401, "未登录或账号不存在");
        Map<String, Object> result = pointService.signIn(userId);
        if (result == null) {
            return Result.fail(400, "今天已经签过到啦，明天再来");
        }
        return Result.ok("签到成功", result);
    }
}

package com.example.aimaster.controller;

import com.example.aimaster.dto.Result;
import com.example.aimaster.service.OssStorageService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 公开配置接口（无需登录）：前端启动时获取全局配置。
 * 当前提供：AI 头像 URL（管理员上传后所有人可见）。
 */
@RestController
@RequestMapping("/api/config")
public class AppConfigController {

    private final OssStorageService ossStorageService;

    public AppConfigController(OssStorageService ossStorageService) {
        this.ossStorageService = ossStorageService;
    }

    /** GET /api/config/ai-avatar 公开：返回当前 AI 头像 URL（未上传时返回约定地址，前端兜底默认图） */
    @GetMapping("/ai-avatar")
    public Result<Map<String, Object>> aiAvatar() {
        Map<String, Object> data = new HashMap<>();
        data.put("avatar", ossStorageService.getAiAvatarUrl());
        return Result.ok(data);
    }
}

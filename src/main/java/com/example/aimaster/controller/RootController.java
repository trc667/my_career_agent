package com.example.aimaster.controller;

import com.example.aimaster.dto.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 根路径：浏览器或健康探测访问 {@code /} 时返回说明，避免走静态资源导致 NoResourceFoundException。
 */
@RestController
public class RootController {

    @GetMapping("/")
    public Result<Map<String, String>> root() {
        return Result.ok(Map.of(
                "service", "ai-career-master",
                "health", "/api/health",
                "hint", "本服务为 REST API，请使用 /api 下接口"
        ));
    }
}

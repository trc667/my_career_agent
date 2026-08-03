package com.example.aimaster.controller;

import com.example.aimaster.dto.AuthResponse;
import com.example.aimaster.dto.LoginRequest;
import com.example.aimaster.dto.RegisterRequest;
import com.example.aimaster.dto.Result;
import com.example.aimaster.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证接口：登录、注册，返回 JWT。
 * Controller 只做参数接收和返回封装，业务逻辑在 AuthServiceImpl。
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public Result<AuthResponse> login(@Valid @RequestBody LoginRequest req) {
        AuthResponse resp = authService.login(req);
        return Result.ok("登录成功", resp);
    }

    @PostMapping("/register")
    public Result<Void> register(@Valid @RequestBody RegisterRequest req) {
        authService.register(req);
        return Result.ok("注册成功", null);
    }
}

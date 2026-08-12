package com.example.aimaster.controller;

import com.example.aimaster.dto.AuthResponse;
import com.example.aimaster.dto.EmailCodeRequest;
import com.example.aimaster.dto.ForgotResetRequest;
import com.example.aimaster.dto.ForgotSendCodeRequest;
import com.example.aimaster.dto.LoginRequest;
import com.example.aimaster.dto.RegisterRequest;
import com.example.aimaster.dto.Result;
import com.example.aimaster.service.AuthService;
import com.example.aimaster.service.EmailCodeService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证接口：登录、注册（含邮箱验证码）、发送验证码。
 * Controller 只做参数接收和返回封装，业务逻辑在 Service。
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final EmailCodeService emailCodeService;

    public AuthController(AuthService authService, EmailCodeService emailCodeService) {
        this.authService = authService;
        this.emailCodeService = emailCodeService;
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

    /** 发送注册邮箱验证码（带 IP 级频率限制） */
    @PostMapping("/send-code")
    public Result<Void> sendCode(@Valid @RequestBody EmailCodeRequest req, HttpServletRequest request) {
        emailCodeService.sendCode(req.getEmail(), request.getRemoteAddr());
        return Result.ok("验证码已发送，请查收邮件", null);
    }

    /** 忘记密码第一步：发送找回密码验证码（按账号注册渠道分发邮箱/短信） */
    @PostMapping("/forgot/send-code")
    public Result<Void> forgotSendCode(@Valid @RequestBody ForgotSendCodeRequest req, HttpServletRequest request) {
        authService.forgotSendCode(req.getAccount(), request.getRemoteAddr());
        return Result.ok("验证码已发送，请查收", null);
    }

    /** 忘记密码第二步：校验验证码并重置密码 */
    @PostMapping("/forgot/reset")
    public Result<Void> forgotReset(@Valid @RequestBody ForgotResetRequest req) {
        authService.forgotReset(req.getAccount(), req.getCode(), req.getNewPassword());
        return Result.ok("密码重置成功，请使用新密码登录", null);
    }
}

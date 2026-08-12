package com.example.aimaster.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 发送找回密码验证码请求体。
 * account 为用户名或注册邮箱（按注册渠道决定发送目标）。
 */
@Data
public class ForgotSendCodeRequest {

    @NotBlank(message = "请输入用户名或邮箱")
    @Size(max = 128, message = "账号长度不超过128")
    private String account;
}

package com.example.aimaster.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 重置密码请求体：账号 + 验证码 + 新密码。
 */
@Data
public class ForgotResetRequest {

    @NotBlank(message = "请输入用户名或邮箱")
    @Size(max = 128, message = "账号长度不超过128")
    private String account;

    @NotBlank(message = "验证码不能为空")
    @Size(min = 6, max = 6, message = "验证码为6位数字")
    private String code;

    @NotBlank(message = "新密码不能为空")
    @Size(min = 6, max = 64, message = "密码长度 6-64")
    private String newPassword;
}

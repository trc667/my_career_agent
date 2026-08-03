package com.example.aimaster.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 修改密码请求体。
 */
@Data
public class ChangePasswordRequest {

    /** 旧密码（必填） */
    @NotBlank(message = "旧密码不能为空")
    private String oldPassword;

    /** 新密码（必填，至少6位） */
    @NotBlank(message = "新密码不能为空")
    @Size(min = 6, max = 64, message = "新密码长度需在6~64位之间")
    private String newPassword;
}

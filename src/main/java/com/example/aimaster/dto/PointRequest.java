package com.example.aimaster.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 管理员发放/扣减积分请求体。
 */
@Data
public class PointRequest {

    @NotNull(message = "用户 ID 不能为空")
    private Long userId;

    /** 变更积分（正数发放/负数扣减，不能为 0） */
    @NotNull(message = "积分变更量不能为空")
    private Integer delta;

    @NotBlank(message = "请填写积分变更原因")
    @Size(max = 128, message = "原因不超过128字符")
    private String reason;
}

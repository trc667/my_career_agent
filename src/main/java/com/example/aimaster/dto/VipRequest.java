package com.example.aimaster.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 管理员开通/续期 VIP 请求体。
 */
@Data
public class VipRequest {

    @NotBlank(message = "用户名不能为空")
    private String username;

    /** 开通天数（从当前时间或现有 VIP 到期时间起算） */
    @NotNull(message = "VIP 天数不能为空")
    @Min(value = 1, message = "VIP 天数必须大于 0")
    private Integer days;
}

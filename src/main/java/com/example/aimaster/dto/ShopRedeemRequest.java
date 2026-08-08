package com.example.aimaster.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 积分商城：兑换请求体。
 */
@Data
public class ShopRedeemRequest {

    /** 商品 ID（/api/shop/items 返回） */
    @NotNull(message = "请选择要兑换的商品")
    private Long itemId;
}

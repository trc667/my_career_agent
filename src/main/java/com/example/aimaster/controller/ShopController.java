package com.example.aimaster.controller;

import java.util.List;
import java.util.Map;

import com.example.aimaster.dto.Result;
import com.example.aimaster.dto.ShopRedeemRequest;
import com.example.aimaster.entity.RedeemRecord;
import com.example.aimaster.entity.User;
import com.example.aimaster.service.AuthService;
import com.example.aimaster.service.ShopService;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

/**
 * 积分商城接口（均需登录）：商品列表 / 兑换 / 我的兑换记录。
 * 积分兑换出口：签到、邀请赚的积分可以兑换资料与 VIP 体验卡。
 */
@RestController
@RequestMapping("/api/shop")
public class ShopController {

    private final ShopService shopService;
    private final AuthService authService;

    public ShopController(ShopService shopService, AuthService authService) {
        this.shopService = shopService;
        this.authService = authService;
    }

    private Long currentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return null;
        User user = authService.getUserInfo(auth.getName());
        return user != null ? user.getId() : null;
    }

    /** GET /api/shop/items 上架商品列表 */
    @GetMapping("/items")
    public Result<List<Map<String, Object>>> items() {
        Long userId = currentUserId();
        if (userId == null) return Result.fail(401, "未登录或账号不存在");
        return Result.ok(shopService.items());
    }

    /** POST /api/shop/redeem 兑换商品（扣积分 + 发放权益） */
    @PostMapping("/redeem")
    public Result<Map<String, Object>> redeem(@Valid @RequestBody ShopRedeemRequest req) {
        Long userId = currentUserId();
        if (userId == null) return Result.fail(401, "未登录或账号不存在");
        return Result.ok(shopService.redeem(userId, req.getItemId()));
    }

    /** GET /api/shop/records 我的兑换记录 */
    @GetMapping("/records")
    public Result<List<RedeemRecord>> records() {
        Long userId = currentUserId();
        if (userId == null) return Result.fail(401, "未登录或账号不存在");
        return Result.ok(shopService.records(userId));
    }
}

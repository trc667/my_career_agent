package com.example.aimaster.service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.example.aimaster.entity.PointLog;
import com.example.aimaster.entity.RedeemItem;
import com.example.aimaster.entity.RedeemRecord;
import com.example.aimaster.entity.User;
import com.example.aimaster.exception.BusinessException;
import com.example.aimaster.mapper.PointLogMapper;
import com.example.aimaster.mapper.RedeemItemMapper;
import com.example.aimaster.mapper.RedeemRecordMapper;
import com.example.aimaster.mapper.UserMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 积分商城：积分兑换出口（断点①修复——签到/邀请赚的积分有地方花）。
 * <p>
 * 设计（面试可讲）：
 * 1) 商品在 DB（可后台维护），类型分 VIP_CARD（兑换即开通 VIP 天数）与 CONTENT（兑换即发放资料正文）；
 * 2) 兑换走原子扣分（UPDATE ... WHERE points >= cost 防并发超扣）+ point_log 流水 + redeem_record 记录双写审计；
 * 3) 纯内部闭环不涉支付，与 admin 手动开 VIP 并列，为后续真实支付预留商品模型。
 */
@Service
public class ShopService {

    private static final Logger log = LoggerFactory.getLogger(ShopService.class);

    /** 商品类型：VIP 体验卡 / 资料内容 */
    public static final String TYPE_VIP_CARD = "VIP_CARD";
    public static final String TYPE_CONTENT = "CONTENT";

    private final RedeemItemMapper redeemItemMapper;
    private final RedeemRecordMapper redeemRecordMapper;
    private final PointLogMapper pointLogMapper;
    private final UserMapper userMapper;
    private final PointService pointService;

    public ShopService(RedeemItemMapper redeemItemMapper, RedeemRecordMapper redeemRecordMapper,
                       PointLogMapper pointLogMapper, UserMapper userMapper, PointService pointService) {
        this.redeemItemMapper = redeemItemMapper;
        this.redeemRecordMapper = redeemRecordMapper;
        this.pointLogMapper = pointLogMapper;
        this.userMapper = userMapper;
        this.pointService = pointService;
    }

    /** 上架商品列表（不含 payload，资料正文兑换成功后才发放） */
    public List<Map<String, Object>> items() {
        return redeemItemMapper.selectList(new LambdaQueryWrapper<RedeemItem>()
                .eq(RedeemItem::getEnabled, 1)
                .orderByAsc(RedeemItem::getSort))
                .stream().map(it -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("id", it.getId());
                    m.put("name", it.getName());
                    m.put("description", it.getDescription());
                    m.put("points", it.getPoints());
                    m.put("type", it.getType());
                    return m;
                }).toList();
    }

    /**
     * 兑换商品：校验 → 原子扣分 → 流水 + 记录双写 → 发放权益。
     * VIP_CARD 调 pointService.grantVip 开通天数；CONTENT 直接返回资料正文。
     */
    @Transactional
    public Map<String, Object> redeem(Long userId, Long itemId) {
        RedeemItem item = redeemItemMapper.selectById(itemId);
        if (item == null || item.getEnabled() == null || item.getEnabled() != 1) {
            throw new BusinessException("商品不存在或已下架");
        }
        User user = userMapper.selectById(userId);
        if (user == null) throw new BusinessException("用户不存在");

        int cost = item.getPoints() == null ? 0 : item.getPoints();
        if (cost <= 0) throw new BusinessException("商品配置异常");

        // 原子扣分：单条 SQL 保证并发下不会扣成负数
        int rows = userMapper.update(null, new UpdateWrapper<User>()
                .setSql("points = points - " + cost)
                .eq("id", userId)
                .ge("points", cost));
        if (rows == 0) {
            throw new BusinessException("积分不足：兑换「" + item.getName() + "」需要 " + cost + " 积分，先到个人中心签到攒分吧");
        }
        pointLogMapper.insert(PointLog.builder()
                .userId(userId).changePoints(-cost).reason("积分商城兑换-" + item.getName())
                .createTime(LocalDateTime.now()).build());

        String payload = item.getPayload() == null ? "" : item.getPayload();
        if (TYPE_VIP_CARD.equals(item.getType())) {
            try {
                int days = Integer.parseInt(payload.trim());
                pointService.grantVip(user.getUsername(), days);
                payload = "已开通 " + days + " 天 VIP（到期时间见个人中心）";
            } catch (NumberFormatException e) {
                throw new BusinessException("商品配置异常，请联系管理员");
            }
        }

        redeemRecordMapper.insert(RedeemRecord.builder()
                .userId(userId).itemId(itemId).itemName(item.getName())
                .points(cost).type(item.getType()).payload(payload)
                .createTime(LocalDateTime.now()).build());

        log.info("积分兑换成功: userId={} item={} cost={}", userId, item.getName(), cost);
        Map<String, Object> m = new HashMap<>();
        m.put("itemName", item.getName());
        m.put("cost", cost);
        m.put("type", item.getType());
        m.put("payload", payload);
        m.put("pointsLeft", Math.max(0, (user.getPoints() == null ? 0 : user.getPoints()) - cost));
        return m;
    }

    /** 我的兑换记录（倒序，最近 50 条） */
    public List<RedeemRecord> records(Long userId) {
        return redeemRecordMapper.selectList(new LambdaQueryWrapper<RedeemRecord>()
                .eq(RedeemRecord::getUserId, userId)
                .orderByDesc(RedeemRecord::getId)
                .last("LIMIT 50"));
    }
}

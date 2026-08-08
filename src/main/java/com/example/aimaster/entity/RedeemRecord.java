package com.example.aimaster.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 积分商城兑换记录：谁/何时/换了什么/花了多少（与 point_log 双写可审计）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("redeem_record")
public class RedeemRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private Long userId;

    @TableField("item_id")
    private Long itemId;

    /** 商品名快照 */
    @TableField("item_name")
    private String itemName;

    /** 本次花费积分 */
    @TableField("points")
    private Integer points;

    /** VIP_CARD / CONTENT */
    @TableField("type")
    private String type;

    /** 发放内容快照（VIP 天数 / 资料正文） */
    @TableField("payload")
    private String payload;

    @TableField("created_at")
    private LocalDateTime createTime;
}

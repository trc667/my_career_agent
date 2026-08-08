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
 * 积分商城商品：积分兑换出口（断点①修复）。
 * type: VIP_CARD=VIP 体验卡（payload 为开通天数）；CONTENT=资料内容（payload 为资料正文）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("redeem_item")
public class RedeemItem {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 商品名 */
    @TableField("name")
    private String name;

    /** 卖点描述 */
    @TableField("description")
    private String description;

    /** 所需积分 */
    @TableField("points")
    private Integer points;

    /** VIP_CARD / CONTENT */
    @TableField("type")
    private String type;

    /** VIP_CARD 为开通天数；CONTENT 为资料正文 */
    @TableField("payload")
    private String payload;

    /** 是否上架 */
    @TableField("enabled")
    private Integer enabled;

    @TableField("sort")
    private Integer sort;

    @TableField("created_at")
    private LocalDateTime createTime;
}

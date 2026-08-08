package com.example.aimaster.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 每日签到表：同用户同日唯一（幂等），points 为当日获得积分。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("sign_in")
public class SignIn {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private Long userId;

    /** 签到日期（同日幂等） */
    @TableField("sign_date")
    private LocalDate signDate;

    /** 当日获得积分 */
    @TableField("points")
    private Integer points;

    @TableField("created_at")
    private LocalDateTime createTime;
}

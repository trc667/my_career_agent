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
 * 用户表：账号密码登录用。
 * 密码存 bcrypt 哈希，不存明文。
 * MyBatis-Plus 注解。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("app_user")
public class User {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("username")
    private String username;

    @TableField("password_hash")
    private String passwordHash;

    /** 注册邮箱（邮箱验证码注册，可空兼容旧用户） */
    @TableField("email")
    private String email;

    /** 角色：USER / ADMIN（默认 USER） */
    @TableField("role")
    private String role;

    /** 头像 URL（阿里云 OSS 地址，可空） */
    @TableField("avatar")
    private String avatar;

    /** 积分余额（签到/反馈/邀请奖励累计） */
    @TableField("points")
    private Integer points;

    /** 会员等级：FREE / VIP（默认 FREE） */
    @TableField("level")
    private String level;

    /** VIP 到期时间（到期后回落到 FREE） */
    @TableField("vip_expire_at")
    private LocalDateTime vipExpireAt;

    @TableField("created_at")
    private LocalDateTime createTime;

    @TableField("updated_at")
    private LocalDateTime updateTime;
}

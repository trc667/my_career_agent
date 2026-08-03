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
 * 意见反馈表：记录用户提交的反馈/需求。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("feedback")
public class Feedback {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 提交反馈的用户名（登录后自动填充） */
    @TableField("username")
    private String username;

    /** 联系方式：邮箱 / 微信号等，可空 */
    @TableField("contact")
    private String contact;

    /** 反馈内容 */
    @TableField("content")
    private String content;

    @TableField("created_at")
    private LocalDateTime createTime;
}

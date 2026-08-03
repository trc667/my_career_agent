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
 * 公告表：系统通知/公告内容。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("announcement")
public class Announcement {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 公告标题 */
    @TableField("title")
    private String title;

    /** 公告内容（支持 \n 换行） */
    @TableField("content")
    private String content;

    @TableField("created_at")
    private LocalDateTime createTime;
}

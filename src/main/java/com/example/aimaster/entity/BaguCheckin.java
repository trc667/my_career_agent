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
 * 八股每日打卡：同用户同日期唯一（幂等）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("bagu_checkin")
public class BaguCheckin {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属用户 ID */
    @TableField("user_id")
    private Long userId;

    /** 打卡日期（yyyy-MM-dd） */
    @TableField("checkin_date")
    private LocalDate checkinDate;

    @TableField("created_at")
    private LocalDateTime createTime;
}

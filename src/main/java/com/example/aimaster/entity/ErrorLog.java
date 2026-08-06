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
 * 错误日志表：自建监控面板的数据源。
 * <p>
 * source 区分 backend（后端异常自动入库）/ frontend（前端全局捕获上报）；
 * 管理后台（ADMIN）可查看/筛选/清空。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("error_log")
public class ErrorLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 级别：ERROR / WARN */
    @TableField("level")
    private String level;

    /** 来源：backend / frontend */
    @TableField("source")
    private String source;

    /** 错误摘要（前端必传，后端取异常 message） */
    @TableField("message")
    private String message;

    /** 完整堆栈/错误详情 */
    @TableField("stack_trace")
    private String stackTrace;

    /** 请求路径（后端从 HttpServletRequest 解析） */
    @TableField("uri")
    private String uri;

    /** 请求方法：GET/POST/... */
    @TableField("method")
    private String method;

    /** 触发用户（登录态时解析，未登录为空） */
    @TableField("username")
    private String username;

    /** 浏览器 UA */
    @TableField("user_agent")
    private String userAgent;

    /** 客户端 IP */
    @TableField("ip")
    private String ip;

    @TableField("created_at")
    private LocalDateTime createTime;
}

package com.example.aimaster.controller;

import com.example.aimaster.dto.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;

/**
 * 数据库诊断接口，用于排查连接问题。排查完毕后可删除或通过配置关闭。
 */
@RestController
@RequestMapping("/api/debug")
public class DbDiagnosticController {

    private final DataSource dataSource;

    public DbDiagnosticController(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * GET /api/debug/db-ping
     * 测试数据库连接，返回详细错误信息便于排查。
     */
    @GetMapping("/db-ping")
    public Result<Map<String, Object>> dbPing() {
        Map<String, Object> info = new HashMap<>();
        try (Connection conn = dataSource.getConnection()) {
            boolean valid = conn.isValid(3);
            info.put("connected", valid);
            info.put("catalog", conn.getCatalog());
            return Result.ok("数据库连接正常", info);
        } catch (Exception e) {
            info.put("connected", false);
            info.put("exceptionType", e.getClass().getName());
            info.put("message", e.getMessage());
            Throwable cause = e.getCause();
            if (cause != null) {
                info.put("causeType", cause.getClass().getName());
                info.put("causeMessage", cause.getMessage());
            }
            return Result.<Map<String, Object>>builder()
                    .code(500)
                    .message("数据库连接失败")
                    .data(info)
                    .build();
        }
    }
}

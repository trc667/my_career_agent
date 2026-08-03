package com.example.aimaster.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 确保 401、500 等错误响应也带上 CORS 头，避免浏览器只显示 CORS 错误。
 * 作为第一道 Filter，在任何响应写入前添加 CORS 头。
 */
@Component
public class CorsHeadersFilter extends OncePerRequestFilter implements Ordered {

    @Value("${app.cors.allowed-origins:}")
    private String allowedOriginsConfig;

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String origin = request.getHeader("Origin");
        if (StringUtils.hasText(origin) && isOriginAllowed(origin)) {
            response.setHeader("Access-Control-Allow-Origin", origin);
            response.setHeader("Access-Control-Allow-Credentials", "true");
            response.setHeader("Access-Control-Expose-Headers", "*");
        }
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            response.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
            response.setHeader("Access-Control-Allow-Headers", "*");
            response.setHeader("Access-Control-Max-Age", "86400");
            response.setStatus(HttpServletResponse.SC_OK);
            return;
        }
        filterChain.doFilter(request, response);
    }

    private boolean isOriginAllowed(String origin) {
        if (!StringUtils.hasText(allowedOriginsConfig)) {
            return true; // 未配置时放行（开发用）
        }
        List<String> allowed = Arrays.stream(allowedOriginsConfig.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .collect(Collectors.toList());
        if (allowed.isEmpty()) return true;
        if (allowed.size() == 1 && "*".equals(allowed.get(0))) return true;
        if (allowed.contains(origin)) return true;
        for (String pattern : allowed) {
            if (pattern.contains("*") && netlifyPatternMatches(pattern, origin)) {
                return true;
            }
        }
        return false;
    }

    /** 与 CorsConfig 中 https://*.netlify.app 等 pattern 对齐 */
    private static boolean netlifyPatternMatches(String pattern, String origin) {
        if (!StringUtils.hasText(origin) || !origin.startsWith("https://")) {
            return false;
        }
        if ("https://*.netlify.app".equals(pattern)) {
            return origin.endsWith(".netlify.app");
        }
        return false;
    }
}

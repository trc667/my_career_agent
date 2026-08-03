package com.example.aimaster.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 跨域配置，允许前端（如 Vite 开发服务器、静态页）访问后端 API。
 * 当 allowCredentials=true 时不能使用 "*"，必须指定具体来源。
 */
@Configuration
public class CorsConfig {

    @Value("${app.cors.allowed-origins:http://localhost:5173,http://localhost:5175,http://localhost:3000,http://127.0.0.1:5173,http://127.0.0.1:5175,http://127.0.0.1:3000,https://*.netlify.app}")
    private String allowedOrigins;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true);
        List<String> origins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .collect(Collectors.toList());
        if (!origins.isEmpty()) {
            if (origins.size() == 1 && "*".equals(origins.get(0))) {
                config.setAllowedOriginPatterns(List.of("*"));
            } else {
                for (String o : origins) {
                    if (o.contains("*")) {
                        config.addAllowedOriginPattern(o);
                    } else {
                        config.addAllowedOrigin(o);
                    }
                }
            }
        } else {
            config.addAllowedOrigin("http://localhost:5173");
            config.addAllowedOrigin("http://127.0.0.1:5173");
        }
        config.addAllowedHeader("*");
        config.addAllowedMethod("*");

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public CorsFilter corsFilter() {
        return new CorsFilter(corsConfigurationSource());
    }
}

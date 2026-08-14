package com.example.aimaster.config;

import com.example.aimaster.security.GuestTrialFilter;
import com.example.aimaster.security.JwtAuthenticationFilter;
import com.example.aimaster.security.RateLimitByLevelFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.http.MediaType;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.nio.charset.StandardCharsets;

/**
 * Spring Security 配置：放行 /api/auth，其余 API 需 JWT。
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final RateLimitByLevelFilter rateLimitByLevelFilter;
    private final GuestTrialFilter guestTrialFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
                          RateLimitByLevelFilter rateLimitByLevelFilter,
                          GuestTrialFilter guestTrialFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.rateLimitByLevelFilter = rateLimitByLevelFilter;
        this.guestTrialFilter = guestTrialFilter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * 禁用 RateLimitByLevelFilter 的 Servlet 容器自动注册。
     * 该过滤器已通过 {@link #securityFilterChain} 的 addFilterAfter 加入 Security 过滤链，
     * 若再被 Spring Boot 自动注册为 Servlet Filter 会在 /api/chat* 上执行两次，导致限流阈值实际减半。
     */
    @Bean
    public FilterRegistrationBean<RateLimitByLevelFilter> rateLimitByLevelFilterRegistration(
            RateLimitByLevelFilter filter) {
        FilterRegistrationBean<RateLimitByLevelFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    /**
     * 禁用 GuestTrialFilter 的 Servlet 容器自动注册（同 RateLimitByLevelFilter，防重复执行）。
     */
    @Bean
    public FilterRegistrationBean<GuestTrialFilter> guestTrialFilterRegistration(GuestTrialFilter filter) {
        FilterRegistrationBean<GuestTrialFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> {})
                .csrf(csrf -> csrf.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex.authenticationEntryPoint((req, res, e) -> {
                    res.setStatus(HttpStatus.UNAUTHORIZED.value());
                    res.setContentType(MediaType.APPLICATION_JSON_VALUE + ";charset=" + StandardCharsets.UTF_8);
                    res.getWriter().write("{\"code\":401,\"message\":\"未登录或 token 已过期\"}");
                }))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**", "/api/health", "/api/announcement/**", "/api/config/**", "/api/monitor/report", "/api/weather/**", "/api/models").permitAll()
                        // 聊天接口放行游客（每日试用 N 次，由 GuestTrialFilter 计数拦截；其余接口仍需 JWT）
                        .requestMatchers("/api/chat", "/api/chat/**").permitAll()
                        .requestMatchers(HttpMethod.OPTIONS).permitAll()
                        .requestMatchers("/error", "/", "/*.html", "/*.js", "/*.css", "/favicon.ico").permitAll()
                        // 管理/诊断接口：仅 ADMIN 角色可访问（db-ping 会泄露连接信息，禁止匿名访问）
                        .requestMatchers("/api/admin/**", "/api/debug/**").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                // 分级限流：在 JWT 认证之后按用户等级限流（VIP/FREE/游客），LLM 聊天路径生效
                .addFilterAfter(rateLimitByLevelFilter, JwtAuthenticationFilter.class)
                // 游客试用：分级限流之后按 IP 消耗每日试用次数（登录用户直接放行）
                .addFilterAfter(guestTrialFilter, RateLimitByLevelFilter.class);
        return http.build();
    }
}

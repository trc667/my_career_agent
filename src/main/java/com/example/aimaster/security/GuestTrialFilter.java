package com.example.aimaster.security;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import com.example.aimaster.service.GuestTrialService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 游客试用过滤器：匿名访问 /api/chat* 时按 IP 消耗每日试用次数，超限返回 429 提示注册。
 * 登录用户（JWT 已认证）直接放行，不消耗试用额度。
 * <p>
 * 注意：本过滤器通过 SecurityConfig 加入 Security 过滤链（在分级限流之后），
 * 并已在 SecurityConfig 中用 FilterRegistrationBean 禁用 Servlet 容器自动注册，避免重复执行。
 */
@Component
public class GuestTrialFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(GuestTrialFilter.class);

    private final GuestTrialService guestTrialService;

    public GuestTrialFilter(GuestTrialService guestTrialService) {
        this.guestTrialService = guestTrialService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path == null || !path.startsWith("/api/chat");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean loggedIn = auth != null && auth.isAuthenticated()
                && !"anonymousUser".equals(String.valueOf(auth.getPrincipal()));
        if (loggedIn) {
            chain.doFilter(request, response);
            return;
        }
        String msg = guestTrialService.tryConsume(request.getRemoteAddr());
        if (msg != null) {
            log.warn("游客试用拦截: ip={} path={}", request.getRemoteAddr(), request.getRequestURI());
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE + ";charset=" + StandardCharsets.UTF_8);
            response.getWriter().write("{\"code\":429,\"message\":\"" + msg + "\"}");
            return;
        }
        chain.doFilter(request, response);
    }
}

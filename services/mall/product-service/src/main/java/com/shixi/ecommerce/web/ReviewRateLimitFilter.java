package com.shixi.ecommerce.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shixi.ecommerce.common.ApiResponse;
import com.shixi.ecommerce.security.JwtUser;
import com.shixi.ecommerce.service.ReviewRateLimitService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * 评价接口限流过滤器。
 *
 * @author shixi
 * @date 2026-03-20
 */
@Component
public class ReviewRateLimitFilter extends OncePerRequestFilter {
    private final ReviewRateLimitService rateLimitService;
    private final ObjectMapper objectMapper;

    public ReviewRateLimitFilter(ReviewRateLimitService rateLimitService, ObjectMapper objectMapper) {
        this.rateLimitService = rateLimitService;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        if (!uri.startsWith("/api/user/reviews")) {
            return true;
        }
        String method = request.getMethod();
        if ("OPTIONS".equalsIgnoreCase(method) || "HEAD".equalsIgnoreCase(method)) {
            return true;
        }
        return !"POST".equalsIgnoreCase(method);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        Long userId = resolveUserId();
        String ip = resolveClientIp(request);

        boolean allowUser = rateLimitService.allowUser(userId);
        boolean allowIp = rateLimitService.allowIp(ip);
        if (!allowUser || !allowIp) {
            response.setStatus(429);
            response.setContentType("application/json;charset=UTF-8");
            ApiResponse<String> payload = ApiResponse.fail("Too many requests");
            response.getWriter().write(objectMapper.writeValueAsString(payload));
            return;
        }
        filterChain.doFilter(request, response);
    }

    private Long resolveUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof JwtUser user) {
            return user.getUserId();
        }
        return null;
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        return request.getRemoteAddr();
    }
}

package com.nowcoder.community.security;

import com.nowcoder.community.util.CommunityUtil;
import com.nowcoder.community.util.CookieUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.http.HttpMethod;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

public class ForumCsrfFilter extends OncePerRequestFilter {
    private final ForumSecurityProperties properties;
    private final ForumCookieService cookieService;

    public ForumCsrfFilter(ForumSecurityProperties properties, ForumCookieService cookieService) {
        this.properties = properties;
        this.cookieService = cookieService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !properties.isCsrfEnabled();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String ticket = CookieUtil.getValue(request, properties.getTicketCookieName());
        if (!StringUtils.hasText(ticket)) {
            filterChain.doFilter(request, response);
            return;
        }

        String csrfToken = cookieService.ensureCsrfCookie(request, response);
        if (requiresCsrfProtection(request)
                && !tokensMatch(request.getHeader(properties.getCsrfHeaderName()), csrfToken)) {
            writeForbidden(request, response);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean requiresCsrfProtection(HttpServletRequest request) {
        String method = request.getMethod();
        return !isExcludedPath(request)
                && !HttpMethod.GET.matches(method)
                && !HttpMethod.HEAD.matches(method)
                && !HttpMethod.OPTIONS.matches(method)
                && !HttpMethod.TRACE.matches(method);
    }

    private boolean tokensMatch(String headerToken, String cookieToken) {
        if (!StringUtils.hasText(headerToken) || !StringUtils.hasText(cookieToken)) {
            return false;
        }
        return MessageDigest.isEqual(
                headerToken.getBytes(StandardCharsets.UTF_8), cookieToken.getBytes(StandardCharsets.UTF_8));
    }

    private void writeForbidden(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        if (isApiRequest(request)) {
            response.setContentType("application/json;charset=utf-8");
            response.getWriter().write(CommunityUtil.getJSONString(403, "CSRF token invalid"));
            return;
        }
        response.sendError(HttpServletResponse.SC_FORBIDDEN, "CSRF token invalid");
    }

    private boolean isApiRequest(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return (uri != null && uri.contains("/api/"))
                || "XMLHttpRequest".equalsIgnoreCase(request.getHeader("x-requested-with"));
    }

    private boolean isExcludedPath(HttpServletRequest request) {
        String uri = request.getRequestURI();
        if (uri == null) {
            return false;
        }
        return uri.endsWith("/login")
                || uri.endsWith("/register")
                || uri.endsWith("/api/session/login")
                || uri.endsWith("/api/session/register");
    }
}

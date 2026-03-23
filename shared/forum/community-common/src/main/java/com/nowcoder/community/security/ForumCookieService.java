package com.nowcoder.community.security;

import com.nowcoder.community.util.CommunityUtil;
import com.nowcoder.community.util.CookieUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.util.StringUtils;

public class ForumCookieService {
    private final ForumSecurityProperties properties;

    public ForumCookieService(ForumSecurityProperties properties) {
        this.properties = properties;
    }

    public void writeTicketCookie(HttpServletResponse response, String ticket, int maxAgeSeconds) {
        addCookie(response, properties.getTicketCookieName(), ticket, Duration.ofSeconds(maxAgeSeconds), true);
    }

    public void clearTicketCookie(HttpServletResponse response) {
        addCookie(response, properties.getTicketCookieName(), "", Duration.ZERO, true);
    }

    public void writeCaptchaCookie(HttpServletResponse response, String captchaOwner, int maxAgeSeconds) {
        addCookie(response, properties.getCaptchaCookieName(), captchaOwner, Duration.ofSeconds(maxAgeSeconds), true);
    }

    public void clearCaptchaCookie(HttpServletResponse response) {
        addCookie(response, properties.getCaptchaCookieName(), "", Duration.ZERO, true);
    }

    public String ensureCsrfCookie(HttpServletRequest request, HttpServletResponse response) {
        String token = CookieUtil.getValue(request, properties.getCsrfCookieName());
        if (StringUtils.hasText(token)) {
            return token;
        }
        token = CommunityUtil.generateUUID();
        addCookie(
                response,
                properties.getCsrfCookieName(),
                token,
                Duration.ofSeconds(properties.getCsrfMaxAgeSeconds()),
                false);
        return token;
    }

    public void clearCsrfCookie(HttpServletResponse response) {
        addCookie(response, properties.getCsrfCookieName(), "", Duration.ZERO, false);
    }

    public String getCsrfToken(HttpServletRequest request) {
        return CookieUtil.getValue(request, properties.getCsrfCookieName());
    }

    private void addCookie(HttpServletResponse response, String name, String value, Duration maxAge, boolean httpOnly) {
        ResponseCookie cookie = ResponseCookie.from(name, value == null ? "" : value)
                .path(properties.getCookiePath())
                .secure(properties.isSecure())
                .httpOnly(httpOnly)
                .sameSite(properties.getSameSite())
                .maxAge(maxAge)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}

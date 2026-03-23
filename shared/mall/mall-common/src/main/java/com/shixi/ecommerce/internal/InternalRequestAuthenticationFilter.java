package com.shixi.ecommerce.internal;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

public class InternalRequestAuthenticationFilter extends OncePerRequestFilter {
    private static final String INTERNAL_ROLE = "ROLE_INTERNAL";

    private final InternalAuthProperties properties;
    private final InternalAuthSigner signer;

    public InternalRequestAuthenticationFilter(InternalAuthProperties properties, InternalAuthSigner signer) {
        this.properties = properties;
        this.signer = signer;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!properties.isEnabled()) {
            return true;
        }
        return !request.getRequestURI().startsWith("/internal/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        CachedBodyHttpServletRequest wrappedRequest = new CachedBodyHttpServletRequest(request);
        String callerServiceId = wrappedRequest.getHeader(InternalAuthHeaders.SERVICE);
        String timestampText = wrappedRequest.getHeader(InternalAuthHeaders.TIMESTAMP);
        String signature = wrappedRequest.getHeader(InternalAuthHeaders.SIGNATURE);

        if (!StringUtils.hasText(callerServiceId)
                || !StringUtils.hasText(timestampText)
                || !StringUtils.hasText(signature)) {
            writeJson(response, HttpServletResponse.SC_UNAUTHORIZED, "Missing internal authentication headers");
            return;
        }

        long timestamp;
        try {
            timestamp = Long.parseLong(timestampText);
        } catch (NumberFormatException ex) {
            writeJson(response, HttpServletResponse.SC_UNAUTHORIZED, "Invalid internal authentication timestamp");
            return;
        }

        if (!signer.isTimestampWithinWindow(timestamp)) {
            writeJson(response, HttpServletResponse.SC_UNAUTHORIZED, "Internal request timestamp expired");
            return;
        }

        String canonicalPath =
                InternalAuthSigner.toCanonicalPath(wrappedRequest.getRequestURI(), wrappedRequest.getQueryString());
        if (!signer.verifySignature(
                callerServiceId,
                wrappedRequest.getMethod(),
                canonicalPath,
                timestamp,
                signature,
                wrappedRequest.getCachedBody())) {
            writeJson(response, HttpServletResponse.SC_UNAUTHORIZED, "Invalid internal request signature");
            return;
        }

        if (!signer.isCallerAllowed(callerServiceId)) {
            writeJson(response, HttpServletResponse.SC_FORBIDDEN, "Caller not allowed for internal API");
            return;
        }

        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                callerServiceId, null, List.of(new SimpleGrantedAuthority(INTERNAL_ROLE)));
        try {
            SecurityContextHolder.getContext().setAuthentication(authentication);
            filterChain.doFilter(wrappedRequest, response);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private void writeJson(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        response.getOutputStream()
                .write(("{\"success\":false,\"message\":\"" + message + "\"}").getBytes(StandardCharsets.UTF_8));
    }
}

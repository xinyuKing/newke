package com.nowcoder.community.internal;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

public class CommunityInternalAuthFilter extends OncePerRequestFilter {
    private final CommunityInternalAuthProperties properties;
    private final CommunityInternalAuthSigner signer;

    public CommunityInternalAuthFilter(CommunityInternalAuthProperties properties, CommunityInternalAuthSigner signer) {
        this.properties = properties;
        this.signer = signer;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!properties.isEnabled()) {
            return true;
        }
        String internalPrefix = request.getContextPath() + "/api/internal/";
        return !request.getRequestURI().startsWith(internalPrefix);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        CachedBodyHttpServletRequest wrappedRequest = new CachedBodyHttpServletRequest(request);
        String callerServiceId = wrappedRequest.getHeader(CommunityInternalAuthSigner.HEADER_SERVICE);
        String timestampText = wrappedRequest.getHeader(CommunityInternalAuthSigner.HEADER_TIMESTAMP);
        String signature = wrappedRequest.getHeader(CommunityInternalAuthSigner.HEADER_SIGNATURE);

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

        String canonicalPath = CommunityInternalAuthSigner.toCanonicalPath(
                wrappedRequest.getRequestURI(), wrappedRequest.getQueryString());
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

        filterChain.doFilter(wrappedRequest, response);
    }

    private void writeJson(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        response.getOutputStream()
                .write(("{\"code\":" + status + ",\"msg\":\"" + message + "\"}").getBytes(StandardCharsets.UTF_8));
    }
}

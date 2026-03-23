package com.shixi.ecommerce.internal;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.util.StringUtils;

public class InternalAuthSigner {
    private static final String HMAC_ALGORITHM = "HmacSHA256";

    private final InternalAuthProperties properties;
    private final String localServiceId;
    private final ThreadLocal<Mac> macHolder;

    public InternalAuthSigner(InternalAuthProperties properties, String applicationName) {
        this.properties = properties;
        this.localServiceId = resolveLocalServiceId(properties, applicationName);
        SecretKeySpec secretKey =
                new SecretKeySpec(properties.getSecret().getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM);
        this.macHolder = ThreadLocal.withInitial(() -> createMac(secretKey));
    }

    public boolean isEnabled() {
        return properties.isEnabled();
    }

    public String getLocalServiceId() {
        return localServiceId;
    }

    public boolean isCallerAllowed(String callerServiceId) {
        List<String> allowedCallers = properties.getAllowedCallers();
        if (allowedCallers == null || allowedCallers.isEmpty()) {
            return true;
        }
        return allowedCallers.stream().anyMatch(callerServiceId::equals);
    }

    public boolean isTimestampWithinWindow(long epochSeconds) {
        long diff = Math.abs(Instant.now().getEpochSecond() - epochSeconds);
        return diff <= Math.max(1L, properties.getMaxSkewSeconds());
    }

    public void applySignature(HttpHeaders headers, HttpMethod method, URI uri, byte[] body) {
        if (!isEnabled()) {
            return;
        }
        long timestamp = Instant.now().getEpochSecond();
        headers.set(InternalAuthHeaders.SERVICE, localServiceId);
        headers.set(InternalAuthHeaders.TIMESTAMP, String.valueOf(timestamp));
        headers.set(
                InternalAuthHeaders.SIGNATURE,
                createSignature(
                        localServiceId, method == null ? "GET" : method.name(), toCanonicalPath(uri), timestamp, body));
    }

    public boolean verifySignature(
            String callerServiceId,
            String method,
            String canonicalPath,
            long timestamp,
            String signature,
            byte[] body) {
        if (!StringUtils.hasText(callerServiceId) || !StringUtils.hasText(signature)) {
            return false;
        }
        String expected = createSignature(callerServiceId, method, canonicalPath, timestamp, body);
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8), signature.getBytes(StandardCharsets.UTF_8));
    }

    public String createSignature(
            String callerServiceId, String method, String canonicalPath, long timestamp, byte[] body) {
        String payload = String.join(
                "\n",
                callerServiceId,
                normalizeMethod(method),
                normalizePath(canonicalPath),
                String.valueOf(timestamp),
                sha256Hex(body));
        Mac mac = macHolder.get();
        mac.reset();
        byte[] digest = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
        return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
    }

    public static String toCanonicalPath(URI uri) {
        if (uri == null) {
            return "/";
        }
        return toCanonicalPath(uri.getRawPath(), uri.getRawQuery());
    }

    public static String toCanonicalPath(String rawPath, String rawQuery) {
        String path = StringUtils.hasText(rawPath) ? rawPath : "/";
        if (!StringUtils.hasText(rawQuery)) {
            return path;
        }
        return path + "?" + rawQuery;
    }

    private static String resolveLocalServiceId(InternalAuthProperties properties, String applicationName) {
        if (StringUtils.hasText(properties.getServiceId())) {
            return properties.getServiceId().trim();
        }
        if (StringUtils.hasText(applicationName)) {
            return applicationName.trim();
        }
        return "unknown-service";
    }

    private static String normalizeMethod(String method) {
        if (!StringUtils.hasText(method)) {
            return "GET";
        }
        return method.trim().toUpperCase(Locale.ROOT);
    }

    private static String normalizePath(String path) {
        if (!StringUtils.hasText(path)) {
            return "/";
        }
        return path.trim();
    }

    private static String sha256Hex(byte[] body) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = body == null ? new byte[0] : body;
            return HexFormat.of().formatHex(digest.digest(bytes));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 algorithm unavailable", ex);
        }
    }

    private static Mac createMac(SecretKeySpec secretKey) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(secretKey);
            return mac;
        } catch (NoSuchAlgorithmException | InvalidKeyException ex) {
            throw new IllegalStateException("Unable to initialize internal auth signer", ex);
        }
    }
}

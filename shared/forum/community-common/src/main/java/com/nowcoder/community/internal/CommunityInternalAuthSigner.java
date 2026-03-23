package com.nowcoder.community.internal;

import feign.RequestTemplate;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Base64;
import java.util.Collection;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.util.StringUtils;

public class CommunityInternalAuthSigner {
    public static final String HEADER_SERVICE = "X-Internal-Service";
    public static final String HEADER_TIMESTAMP = "X-Internal-Timestamp";
    public static final String HEADER_SIGNATURE = "X-Internal-Signature";
    private static final String HMAC_ALGORITHM = "HmacSHA256";

    private final CommunityInternalAuthProperties properties;
    private final String localServiceId;
    private final ThreadLocal<Mac> macHolder;

    public CommunityInternalAuthSigner(CommunityInternalAuthProperties properties, String applicationName) {
        this.properties = properties;
        this.localServiceId = resolveLocalServiceId(properties, applicationName);
        SecretKeySpec secretKey =
                new SecretKeySpec(properties.getSecret().getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM);
        this.macHolder = ThreadLocal.withInitial(() -> createMac(secretKey));
    }

    public boolean isEnabled() {
        return properties.isEnabled();
    }

    public boolean isTimestampWithinWindow(long epochSeconds) {
        long diff = Math.abs(Instant.now().getEpochSecond() - epochSeconds);
        return diff <= Math.max(1L, properties.getMaxSkewSeconds());
    }

    public boolean isCallerAllowed(String callerServiceId) {
        List<String> allowedCallers = properties.getAllowedCallers();
        if (allowedCallers == null || allowedCallers.isEmpty()) {
            return true;
        }
        return allowedCallers.stream().anyMatch(callerServiceId::equals);
    }

    public void applySignature(RequestTemplate template) {
        if (!isEnabled()) {
            return;
        }
        long timestamp = Instant.now().getEpochSecond();
        String canonicalPath = toCanonicalPath(template.path(), template.queries());
        byte[] body = template.body() == null ? new byte[0] : template.body();
        template.header(HEADER_SERVICE, localServiceId);
        template.header(HEADER_TIMESTAMP, String.valueOf(timestamp));
        template.header(
                HEADER_SIGNATURE, createSignature(localServiceId, template.method(), canonicalPath, timestamp, body));
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

    public static String toCanonicalPath(String rawPath, String rawQuery) {
        String path = StringUtils.hasText(rawPath) ? rawPath : "/";
        if (!StringUtils.hasText(rawQuery)) {
            return path;
        }
        return path + "?" + rawQuery;
    }

    public static String toCanonicalPath(String rawPath, Map<String, Collection<String>> queries) {
        String path = normalizePath(rawPath);
        StringBuilder query = new StringBuilder();
        int separatorIndex = path.indexOf('?');
        if (separatorIndex >= 0) {
            query.append(path.substring(separatorIndex + 1));
            path = path.substring(0, separatorIndex);
        }
        if (queries == null || queries.isEmpty()) {
            return toCanonicalPath(path, query.length() == 0 ? null : query.toString());
        }
        for (Map.Entry<String, Collection<String>> entry : queries.entrySet()) {
            for (String value : entry.getValue()) {
                if (query.length() > 0) {
                    query.append('&');
                }
                query.append(entry.getKey()).append('=').append(value);
            }
        }
        return toCanonicalPath(path, query.toString());
    }

    private static String resolveLocalServiceId(CommunityInternalAuthProperties properties, String applicationName) {
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

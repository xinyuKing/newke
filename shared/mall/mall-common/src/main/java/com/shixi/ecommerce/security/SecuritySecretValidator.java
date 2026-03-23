package com.shixi.ecommerce.security;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import org.springframework.util.StringUtils;

public final class SecuritySecretValidator {
    private static final List<String> PLACEHOLDER_MARKERS =
            List.of("change-this", "replace-this", "replace-me", "your-secret", "default-secret");

    private SecuritySecretValidator() {}

    public static String requireStrongSecret(String secret, int minimumBytes, String propertyName) {
        if (!StringUtils.hasText(secret)) {
            throw new IllegalStateException(propertyName + " must be configured");
        }
        String normalized = secret.trim();
        if (isPlaceholder(normalized)) {
            throw new IllegalStateException(propertyName + " must not use a placeholder value");
        }
        int length = normalized.getBytes(StandardCharsets.UTF_8).length;
        if (length < minimumBytes) {
            throw new IllegalStateException(propertyName + " must be at least " + minimumBytes + " bytes");
        }
        return normalized;
    }

    public static String defaultIfBlank(String secret, String fallback) {
        if (!StringUtils.hasText(secret)) {
            return fallback;
        }
        return secret.trim();
    }

    private static boolean isPlaceholder(String secret) {
        String normalized = secret.toLowerCase(Locale.ROOT);
        return PLACEHOLDER_MARKERS.stream().anyMatch(normalized::contains);
    }
}

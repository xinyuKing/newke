package com.shixi.ecommerce.internal;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class InternalAuthSignerTest {
    @Test
    void shouldVerifyMatchingSignature() {
        InternalAuthProperties properties = new InternalAuthProperties();
        properties.setServiceId("cart-service");
        properties.setSecret("test-internal-secret-32-bytes-123456");
        InternalAuthSigner signer = new InternalAuthSigner(properties, "cart-service");

        byte[] body = "{\"userId\":42}".getBytes(StandardCharsets.UTF_8);
        long timestamp = Instant.now().getEpochSecond();
        String signature = signer.createSignature("cart-service", "POST", "/internal/orders", timestamp, body);

        assertTrue(signer.verifySignature("cart-service", "POST", "/internal/orders", timestamp, signature, body));
        assertTrue(signer.isTimestampWithinWindow(timestamp));
    }

    @Test
    void shouldRejectTamperedBody() {
        InternalAuthProperties properties = new InternalAuthProperties();
        properties.setServiceId("cart-service");
        properties.setSecret("test-internal-secret-32-bytes-123456");
        InternalAuthSigner signer = new InternalAuthSigner(properties, "cart-service");

        long timestamp = Instant.now().getEpochSecond();
        String signature = signer.createSignature(
                "cart-service",
                "POST",
                "/internal/orders",
                timestamp,
                "{\"userId\":42}".getBytes(StandardCharsets.UTF_8));

        assertFalse(signer.verifySignature(
                "cart-service",
                "POST",
                "/internal/orders",
                timestamp,
                signature,
                "{\"userId\":43}".getBytes(StandardCharsets.UTF_8)));
    }
}

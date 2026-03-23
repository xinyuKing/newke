package com.nowcoder.community.internal;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class CommunityInternalAuthSignerTest {
    @Test
    void shouldVerifyMatchingSignature() {
        CommunityInternalAuthProperties properties = new CommunityInternalAuthProperties();
        properties.setServiceId("user-service");
        properties.setSecret("test-internal-secret-32-bytes-123456");
        CommunityInternalAuthSigner signer = new CommunityInternalAuthSigner(properties, "user-service");

        byte[] body = "{\"offset\":0}".getBytes(StandardCharsets.UTF_8);
        long timestamp = Instant.now().getEpochSecond();
        String signature = signer.createSignature(
                "user-service", "GET", "/community/api/internal/posts/user/1?offset=0&limit=10", timestamp, body);

        assertTrue(signer.verifySignature(
                "user-service",
                "GET",
                "/community/api/internal/posts/user/1?offset=0&limit=10",
                timestamp,
                signature,
                body));
    }

    @Test
    void shouldRejectDifferentPath() {
        CommunityInternalAuthProperties properties = new CommunityInternalAuthProperties();
        properties.setServiceId("message-service");
        properties.setSecret("test-internal-secret-32-bytes-123456");
        CommunityInternalAuthSigner signer = new CommunityInternalAuthSigner(properties, "message-service");

        long timestamp = Instant.now().getEpochSecond();
        String signature = signer.createSignature(
                "message-service", "DELETE", "/community/api/internal/search/index/1", timestamp, new byte[0]);

        assertFalse(signer.verifySignature(
                "message-service",
                "DELETE",
                "/community/api/internal/search/index/2",
                timestamp,
                signature,
                new byte[0]));
    }
}

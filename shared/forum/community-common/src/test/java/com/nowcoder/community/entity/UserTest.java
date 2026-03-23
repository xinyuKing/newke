package com.nowcoder.community.entity;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class UserTest {
    @Test
    void toStringShouldRedactSensitiveFields() {
        User user = new User();
        user.setUsername("alice");
        user.setPassword("secret");
        user.setSalt("salt");
        user.setEmail("alice@example.com");
        user.setActivationCode("code");

        String text = user.toString();

        assertTrue(text.contains("[REDACTED]"));
        assertFalse(text.contains("secret"));
        assertFalse(text.contains("alice@example.com"));
        assertFalse(text.contains("code"));
    }
}

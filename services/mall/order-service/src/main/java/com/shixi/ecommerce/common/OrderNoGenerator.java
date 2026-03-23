package com.shixi.ecommerce.common;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class OrderNoGenerator {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");
    private static final char[] RANDOM_ALPHABET = "0123456789ABCDEFGHJKMNPQRSTVWXYZ".toCharArray();
    private static final int RANDOM_PART_LENGTH = 12;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    public static String nextOrderNo() {
        String timePart = LocalDateTime.now().format(FORMATTER);
        StringBuilder builder = new StringBuilder(timePart.length() + RANDOM_PART_LENGTH);
        builder.append(timePart);
        for (int i = 0; i < RANDOM_PART_LENGTH; i++) {
            builder.append(RANDOM_ALPHABET[SECURE_RANDOM.nextInt(RANDOM_ALPHABET.length)]);
        }
        return builder.toString();
    }
}

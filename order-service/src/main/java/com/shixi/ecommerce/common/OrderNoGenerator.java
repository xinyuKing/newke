package com.shixi.ecommerce.common;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

public class OrderNoGenerator {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    public static String nextOrderNo() {
        String timePart = LocalDateTime.now().format(FORMATTER);
        int rand = ThreadLocalRandom.current().nextInt(100, 999);
        return timePart + rand;
    }
}

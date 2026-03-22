package com.shixi.ecommerce.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * E-commerce accounts are sourced from community-user-service.
 * Local bootstrap is intentionally disabled to keep IDs aligned.
 */
@Configuration
public class DataInitializer {
    @Bean
    public CommandLineRunner initUsers() {
        return args -> {
            // no-op
        };
    }
}

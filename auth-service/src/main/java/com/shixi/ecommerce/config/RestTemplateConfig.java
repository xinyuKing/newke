package com.shixi.ecommerce.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * Centralized HTTP client configuration for cross-service calls.
 *
 * <p>The mall auth service currently uses this {@link RestTemplate} to call the forum user
 * service. Keeping the bean in a dedicated configuration class makes future timeout, interceptor
 * and tracing customization easier.</p>
 */
@Configuration
public class RestTemplateConfig {
    /**
     * Creates the shared {@link RestTemplate} instance used by integration clients.
     *
     * @param builder Spring Boot builder with framework defaults
     * @return configured HTTP client
     */
    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder.build();
    }
}

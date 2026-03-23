package com.shixi.ecommerce.config;

import com.shixi.ecommerce.internal.InternalAuthProperties;
import com.shixi.ecommerce.internal.InternalAuthRestTemplateInterceptor;
import com.shixi.ecommerce.internal.InternalAuthSigner;
import com.shixi.ecommerce.network.ClientIpProperties;
import com.shixi.ecommerce.network.ClientIpResolver;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration
public class MallCommonInfrastructureConfiguration {
    @Bean
    @ConfigurationProperties(prefix = "internal.auth")
    public InternalAuthProperties internalAuthProperties() {
        return new InternalAuthProperties();
    }

    @Bean
    public InternalAuthSigner internalAuthSigner(InternalAuthProperties properties, Environment environment) {
        return new InternalAuthSigner(properties, environment.getProperty("spring.application.name"));
    }

    @Bean
    public InternalAuthRestTemplateInterceptor internalAuthRestTemplateInterceptor(InternalAuthSigner signer) {
        return new InternalAuthRestTemplateInterceptor(signer);
    }

    @Bean
    @ConfigurationProperties(prefix = "security.client-ip")
    public ClientIpProperties clientIpProperties() {
        return new ClientIpProperties();
    }

    @Bean
    public ClientIpResolver clientIpResolver(ClientIpProperties properties) {
        return new ClientIpResolver(properties);
    }
}

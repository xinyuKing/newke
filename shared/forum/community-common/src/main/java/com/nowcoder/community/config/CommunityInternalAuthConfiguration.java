package com.nowcoder.community.config;

import com.nowcoder.community.internal.CommunityInternalAuthFeignInterceptor;
import com.nowcoder.community.internal.CommunityInternalAuthFilter;
import com.nowcoder.community.internal.CommunityInternalAuthProperties;
import com.nowcoder.community.internal.CommunityInternalAuthSigner;
import feign.RequestInterceptor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.env.Environment;

@Configuration
public class CommunityInternalAuthConfiguration {
    @Bean
    @ConfigurationProperties(prefix = "internal.auth")
    public CommunityInternalAuthProperties communityInternalAuthProperties() {
        return new CommunityInternalAuthProperties();
    }

    @Bean
    public CommunityInternalAuthSigner communityInternalAuthSigner(
            CommunityInternalAuthProperties properties, Environment environment) {
        return new CommunityInternalAuthSigner(properties, environment.getProperty("spring.application.name"));
    }

    @Bean
    public RequestInterceptor communityInternalAuthFeignInterceptor(CommunityInternalAuthSigner signer) {
        return new CommunityInternalAuthFeignInterceptor(signer);
    }

    @Bean
    public FilterRegistrationBean<CommunityInternalAuthFilter> communityInternalAuthFilterRegistration(
            CommunityInternalAuthProperties properties, CommunityInternalAuthSigner signer) {
        FilterRegistrationBean<CommunityInternalAuthFilter> registration =
                new FilterRegistrationBean<>(new CommunityInternalAuthFilter(properties, signer));
        registration.addUrlPatterns("/api/internal/*");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registration;
    }
}

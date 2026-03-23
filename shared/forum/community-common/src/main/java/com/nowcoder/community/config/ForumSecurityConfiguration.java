package com.nowcoder.community.config;

import com.nowcoder.community.security.ForumCookieService;
import com.nowcoder.community.security.ForumCsrfFilter;
import com.nowcoder.community.security.ForumSecurityProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

@Configuration
public class ForumSecurityConfiguration {
    @Bean
    @ConfigurationProperties(prefix = "community.security")
    public ForumSecurityProperties forumSecurityProperties() {
        return new ForumSecurityProperties();
    }

    @Bean
    public ForumCookieService forumCookieService(ForumSecurityProperties properties) {
        return new ForumCookieService(properties);
    }

    @Bean
    public FilterRegistrationBean<ForumCsrfFilter> forumCsrfFilterRegistration(
            ForumSecurityProperties properties, ForumCookieService cookieService) {
        FilterRegistrationBean<ForumCsrfFilter> registration =
                new FilterRegistrationBean<>(new ForumCsrfFilter(properties, cookieService));
        registration.addUrlPatterns("/*");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 10);
        return registration;
    }
}

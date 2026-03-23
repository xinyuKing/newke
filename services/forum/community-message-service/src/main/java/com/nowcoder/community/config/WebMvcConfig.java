package com.nowcoder.community.config;

import com.nowcoder.community.controller.interceptor.MessageInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 消息服务 Web MVC 配置。
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final MessageInterceptor messageInterceptor;

    public WebMvcConfig(MessageInterceptor messageInterceptor) {
        this.messageInterceptor = messageInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(messageInterceptor).addPathPatterns("/**").excludePathPatterns("/error");
    }
}

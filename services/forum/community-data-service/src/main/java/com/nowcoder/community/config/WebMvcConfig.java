package com.nowcoder.community.config;

import com.nowcoder.community.controller.interceptor.DataInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 配置。
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final DataInterceptor dataInterceptor;

    public WebMvcConfig(DataInterceptor dataInterceptor) {
        this.dataInterceptor = dataInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(dataInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns("/error");
    }
}

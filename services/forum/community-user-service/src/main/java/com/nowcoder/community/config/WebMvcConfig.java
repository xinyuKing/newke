package com.nowcoder.community.config;

import com.nowcoder.community.controller.interceptor.LoginRequiredInterceptor;
import com.nowcoder.community.controller.interceptor.LoginTicketInterceptor;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 用户服务 Web MVC 配置。
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final LoginTicketInterceptor loginTicketInterceptor;
    private final LoginRequiredInterceptor loginRequiredInterceptor;
    private final String corsAllowedOrigins;

    public WebMvcConfig(
            LoginTicketInterceptor loginTicketInterceptor,
            LoginRequiredInterceptor loginRequiredInterceptor,
            @Value("${community.cors.allowed-origins:http://localhost:5173,http://127.0.0.1:5173}")
                    String corsAllowedOrigins) {
        this.loginTicketInterceptor = loginTicketInterceptor;
        this.loginRequiredInterceptor = loginRequiredInterceptor;
        this.corsAllowedOrigins = corsAllowedOrigins;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(loginTicketInterceptor)
                .excludePathPatterns("/**/*.css", "/**/*.js", "/**/*.jpg", "/**/*.jpeg", "/**/*.png");

        // 登录校验拦截器必须注册后，@LoginRequired 才会真正生效。
        registry.addInterceptor(loginRequiredInterceptor)
                .excludePathPatterns("/**/*.css", "/**/*.js", "/**/*.jpg", "/**/*.jpeg", "/**/*.png", "/error");
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        String[] origins = resolveAllowedOrigins();
        registry.addMapping("/api/**")
                .allowedOrigins(origins)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);

        registry.addMapping("/kaptcha")
                .allowedOrigins(origins)
                .allowedMethods("GET")
                .allowCredentials(true)
                .maxAge(3600);
    }

    /**
     * 清洗允许跨域的来源列表，避免空字符串污染 Spring MVC 配置。
     *
     * @return 允许的来源数组
     */
    private String[] resolveAllowedOrigins() {
        String[] rawOrigins = corsAllowedOrigins.split(",");
        List<String> origins = new ArrayList<>();
        for (String rawOrigin : rawOrigins) {
            String origin = rawOrigin == null ? null : rawOrigin.trim();
            if (origin != null && !origin.isEmpty()) {
                origins.add(origin);
            }
        }
        return origins.toArray(new String[0]);
    }
}

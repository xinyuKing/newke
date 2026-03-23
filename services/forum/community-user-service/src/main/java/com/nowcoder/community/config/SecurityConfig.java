package com.nowcoder.community.config;

import com.nowcoder.community.util.CommunityConstant;
import com.nowcoder.community.util.CommunityUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.web.SecurityFilterChain;

import java.io.IOException;
import java.io.PrintWriter;

@Configuration
@EnableWebSecurity
public class SecurityConfig implements CommunityConstant {

    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        return web -> web.ignoring().requestMatchers("/resource/**");
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(
                                "/user/setting",
                                "/user/upload",
                                "/discuss/add",
                                "/comment/add/**",
                                "/letter/**",
                                "/notice/**",
                                "/like",
                                "/follow",
                                "/unfollow",
                                "/profile/**",
                                "/followees/**",
                                "/followers/**"
                        ).hasAnyAuthority(AUTHORITY_USER, AUTHORITY_ADMIN, AUTHORITY_MODERATOR)
                        .requestMatchers("/discuss/top", "/discuss/wonderful").hasAnyAuthority(AUTHORITY_MODERATOR)
                        .requestMatchers("/discuss/delete", "/data/**", "/actuator/**").hasAnyAuthority(AUTHORITY_ADMIN)
                        .anyRequest().permitAll())
                .csrf(csrf -> csrf.disable())
                .exceptionHandling(exceptionHandling -> exceptionHandling
                        .authenticationEntryPoint(this::handleAuthenticationEntryPoint)
                        .accessDeniedHandler(this::handleAccessDenied))
                .logout(logout -> logout.logoutUrl("/securitylogout"));

        return http.build();
    }

    private void handleAuthenticationEntryPoint(HttpServletRequest request,
                                                HttpServletResponse response,
                                                org.springframework.security.core.AuthenticationException ex)
            throws IOException {
        if (isAjaxRequest(request)) {
            writePlainResponse(response, CommunityUtil.getJSONString(403, "\u60a8\u8fd8\u6ca1\u6709\u767b\u5f55!"));
            return;
        }

        response.sendRedirect(request.getContextPath() + "/login");
    }

    private void handleAccessDenied(HttpServletRequest request,
                                    HttpServletResponse response,
                                    org.springframework.security.access.AccessDeniedException ex)
            throws IOException {
        if (isAjaxRequest(request)) {
            writePlainResponse(response, CommunityUtil.getJSONString(403, "\u4f60\u6ca1\u6709\u8bbf\u95ee\u6b64\u529f\u80fd\u7684\u6743\u9650!"));
            return;
        }

        response.sendRedirect(request.getContextPath() + "/denied");
    }

    private boolean isAjaxRequest(HttpServletRequest request) {
        return "XMLHttpRequest".equals(request.getHeader("x-requested-with"));
    }

    private void writePlainResponse(HttpServletResponse response, String body) throws IOException {
        response.setContentType("application/json;charset=utf-8");
        try (PrintWriter writer = response.getWriter()) {
            writer.write(body);
        }
    }
}

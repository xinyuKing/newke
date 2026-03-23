package com.shixi.ecommerce.config;

import com.shixi.ecommerce.internal.InternalAuthProperties;
import com.shixi.ecommerce.internal.InternalAuthSigner;
import com.shixi.ecommerce.internal.InternalRequestAuthenticationFilter;
import com.shixi.ecommerce.security.JwtAuthFilter;
import com.shixi.ecommerce.web.ReviewRateLimitFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@EnableMethodSecurity
@Configuration
public class SecurityConfig {
    private final JwtAuthFilter jwtAuthFilter;
    private final ReviewRateLimitFilter reviewRateLimitFilter;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter, ReviewRateLimitFilter reviewRateLimitFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.reviewRateLimitFilter = reviewRateLimitFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http, InternalRequestAuthenticationFilter internalRequestAuthenticationFilter)
            throws Exception {
        http.csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.requestMatchers("/internal/**")
                        .hasRole("INTERNAL")
                        .requestMatchers("/api/products/**", "/actuator/health")
                        .permitAll()
                        .requestMatchers("/api/user/**")
                        .hasRole("USER")
                        .requestMatchers("/api/merchant/**")
                        .hasRole("MERCHANT")
                        .requestMatchers("/api/admin/**")
                        .hasRole("ADMIN")
                        .anyRequest()
                        .authenticated())
                .addFilterBefore(internalRequestAuthenticationFilter, JwtAuthFilter.class)
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(reviewRateLimitFilter, JwtAuthFilter.class);
        return http.build();
    }

    @Bean
    public InternalRequestAuthenticationFilter internalRequestAuthenticationFilter(
            InternalAuthProperties internalAuthProperties, InternalAuthSigner internalAuthSigner) {
        return new InternalRequestAuthenticationFilter(internalAuthProperties, internalAuthSigner);
    }
}

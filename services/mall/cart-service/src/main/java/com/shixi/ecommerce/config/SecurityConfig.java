package com.shixi.ecommerce.config;

import com.shixi.ecommerce.security.JwtAuthFilter;
import com.shixi.ecommerce.web.CartRateLimitFilter;
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
    private final CartRateLimitFilter cartRateLimitFilter;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter,
                          CartRateLimitFilter cartRateLimitFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.cartRateLimitFilter = cartRateLimitFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/user/**", "/actuator/health").hasRole("USER")
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(cartRateLimitFilter, JwtAuthFilter.class);
        return http.build();
    }
}

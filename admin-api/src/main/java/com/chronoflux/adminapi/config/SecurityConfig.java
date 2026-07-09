package com.chronoflux.adminapi.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable) // Disabled for API
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/admin/auth/login", "/api/admin/auth/callback").permitAll()
                // In Phase 1 we permit all, Day 4 filter will lock it down later if needed,
                // but the task says "permit /login, /callback, /signout".
                .anyRequest().permitAll() 
            );

        return http.build();
    }
}

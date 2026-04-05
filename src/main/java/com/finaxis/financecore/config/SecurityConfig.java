package com.finaxis.financecore.config;

import com.finaxis.financecore.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter filter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http.csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth

                        .requestMatchers(
                                "/api/auth/**",

                                // ✅ Swagger (ALL required)
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html",

                                // ✅ Optional (covers edge cases)
                                "/swagger-resources/**",
                                "/webjars/**"
                        ).permitAll()

                        .requestMatchers("/api/users/**").hasAuthority("ADMIN")

                        .requestMatchers("/api/records/**")
                        .hasAnyAuthority("ADMIN", "ANALYST")

                        .anyRequest().authenticated()
                )
                .addFilterBefore(filter,
                        org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
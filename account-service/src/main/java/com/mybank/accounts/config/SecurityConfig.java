package com.mybank.accounts.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;

@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // для простого MVP отключаем CSRF (иначе POST/PUT могут упереться)
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/**").permitAll()
                        .anyRequest().authenticated()
                )
                // наш фильтр подставляет Authentication
                .addFilterBefore(alwaysAliceFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    OncePerRequestFilter alwaysAliceFilter() {
        return new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(
                    HttpServletRequest request,
                    HttpServletResponse response,
                    FilterChain filterChain
            ) throws java.io.IOException, jakarta.servlet.ServletException {

                // Подставляем "alice" как текущего пользователя
                var auth = new UsernamePasswordAuthenticationToken(
                        "alice",
                        "N/A",
                        List.of(new SimpleGrantedAuthority("ROLE_USER"))
                );

                org.springframework.security.core.context.SecurityContextHolder.getContext()
                        .setAuthentication(auth);

                filterChain.doFilter(request, response);
            }
        };
    }
}

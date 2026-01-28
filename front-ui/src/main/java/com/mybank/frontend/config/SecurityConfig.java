package com.mybank.frontend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Конфигурация Security для фронтенда
 * 
 * ТЕКУЩИЙ РЕЖИМ: БЕЗ KEYCLOAK (заглушка для разработки)
 * - Простая форма логина
 * - Пользователь: alice / password (из application.yml)
 * 
 * ДЛЯ KEYCLOAK: Раскомментируйте секцию oauth2Login ниже
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Настройка авторизации запросов
            .authorizeHttpRequests(authz -> authz
                .requestMatchers("/css/**", "/js/**", "/images/**", "/error", "/login").permitAll()
                .anyRequest().authenticated()
            )
            
            // ВАРИАНТ 1: БЕЗ KEYCLOAK (текущий режим)
            // Простая форма логина
            .formLogin(form -> form
                .loginPage("/login")
                .defaultSuccessUrl("/", true)
                .permitAll()
            )
            
            // ВАРИАНТ 2: С KEYCLOAK (закомментировано)
            // Раскомментируйте когда Keycloak настроен
            // 
            // .oauth2Login(oauth2 -> oauth2
            //     .loginPage("/login")
            //     .defaultSuccessUrl("/", true)
            //     .failureUrl("/login?error=true")
            // )
            
            // Настройка Logout
            .logout(logout -> logout
                .logoutSuccessUrl("/login?logout")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
            );

        return http.build();
    }
}


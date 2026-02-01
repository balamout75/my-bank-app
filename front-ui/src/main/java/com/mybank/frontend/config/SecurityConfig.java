package com.mybank.frontend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.client.oidc.web.logout.OidcClientInitiatedLogoutSuccessHandler;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;

/**
 * Конфигурация Security для фронтенда
 * 
 * ТЕКУЩИЙ РЕЖИМ: БЕЗ KEYCLOAK (заглушка для разработки)
 * - Простая форма логина
 * - Пользователь: alice / password (из application.yml.bak)
 * 
 * ДЛЯ KEYCLOAK: Раскомментируйте секцию oauth2Login ниже
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public LogoutSuccessHandler oidcLogoutSuccessHandler(
            ClientRegistrationRepository clientRegistrationRepository
    ) {
        var handler = new OidcClientInitiatedLogoutSuccessHandler(clientRegistrationRepository);
        handler.setPostLogoutRedirectUri("{baseUrl}/login?logout");
        return handler;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   LogoutSuccessHandler oidcLogoutSuccessHandler) throws Exception {
        http
            // Настройка авторизации запросов
            .authorizeHttpRequests(authz -> authz
                .requestMatchers("/css/**", "/js/**", "/images/**", "/error").permitAll()
                .anyRequest().authenticated()
            )
            .oauth2Login(oauth2 -> oauth2
                .defaultSuccessUrl("/", true)
            )
            
            // Настройка Logout
            .logout(logout -> logout
                .logoutSuccessHandler(oidcLogoutSuccessHandler)
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
            );

        return http.build();
    }
}


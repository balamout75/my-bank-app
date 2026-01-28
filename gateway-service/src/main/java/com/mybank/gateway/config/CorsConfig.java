package com.mybank.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * CORS конфигурация для Gateway
 * 
 * Настраивает Cross-Origin Resource Sharing для frontend приложений
 * 
 * В production необходимо:
 * - Указать конкретные allowed origins (не *)
 * - Ограничить allowed methods если нужно
 * - Настроить allowed headers
 */
@Configuration
public class CorsConfig {

    @Bean
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration corsConfig = new CorsConfiguration();
        
        // Разрешенные origins
        // В production заменить на конкретные домены!
        corsConfig.setAllowedOriginPatterns(List.of("*"));
        
        // Разрешенные HTTP методы
        corsConfig.setAllowedMethods(Arrays.asList(
            "GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"
        ));
        
        // Разрешенные headers
        corsConfig.setAllowedHeaders(List.of("*"));
        
        // Разрешить credentials (cookies, authorization headers)
        corsConfig.setAllowCredentials(true);
        
        // Максимальное время кэширования preflight запроса
        corsConfig.setMaxAge(3600L);
        
        // Какие headers можно отдать клиенту
        corsConfig.setExposedHeaders(Arrays.asList(
            "X-Request-ID", 
            "X-Response-Time"
        ));
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfig);
        
        return new CorsWebFilter(source);
    }
}

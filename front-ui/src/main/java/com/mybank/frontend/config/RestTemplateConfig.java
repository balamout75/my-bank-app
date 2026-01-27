package com.mybank.frontend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.web.client.RestTemplate;

/**
 * Конфигурация RestTemplate
 * Автоматически добавляет OAuth2 токен к каждому запросу
 */
@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate(OAuth2AuthorizedClientService authorizedClientService) {
        RestTemplate restTemplate = new RestTemplate();
        
        // Добавляем interceptor для автоматической подстановки токена
        restTemplate.getInterceptors().add((request, body, execution) -> {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            
            if (authentication instanceof OAuth2AuthenticationToken oauthToken) {
                // Получаем авторизованного клиента
                OAuth2AuthorizedClient client = authorizedClientService.loadAuthorizedClient(
                    oauthToken.getAuthorizedClientRegistrationId(),
                    oauthToken.getName()
                );
                
                // Добавляем токен в заголовок
                if (client != null && client.getAccessToken() != null) {
                    String token = client.getAccessToken().getTokenValue();
                    request.getHeaders().setBearerAuth(token);
                }
            }
            
            return execution.execute(request, body);
        });
        
        return restTemplate;
    }
}

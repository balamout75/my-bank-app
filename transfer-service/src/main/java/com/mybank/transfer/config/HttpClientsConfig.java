package com.mybank.transfer.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.security.oauth2.client.*;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.web.client.RestClient;

@Configuration
public class HttpClientsConfig {

    @Bean
    public OAuth2AuthorizedClientManager authorizedClientManager(
            ClientRegistrationRepository registrations,
            OAuth2AuthorizedClientService clientService
    ) {
        OAuth2AuthorizedClientProvider provider = OAuth2AuthorizedClientProviderBuilder.builder()
                .clientCredentials()
                .build();

        var manager = new AuthorizedClientServiceOAuth2AuthorizedClientManager(registrations, clientService);
        manager.setAuthorizedClientProvider(provider);
        return manager;
    }

    // 1) PLAIN builder — по умолчанию (важно!)
    @Bean
    @Primary
    public RestClient.Builder restClientBuilder() {
        return RestClient.builder();
    }

    // 2) LB builder — только для lb://...
    @Bean("loadBalancedRestClientBuilder")
    @LoadBalanced
    public RestClient.Builder loadBalancedRestClientBuilder() {
        return RestClient.builder();
    }

    @Bean("accountsRestClient")
    public RestClient accountsRestClient(
            @Qualifier("loadBalancedRestClientBuilder") RestClient.Builder lb,
            OAuth2AuthorizedClientManager manager
    ) {
        return lb.baseUrl("lb://accounts-service")
                .requestInterceptor(new OAuth2ClientCredentialsInterceptor(manager, "transfer-service"))
                .build();
    }

    @Bean("notificationsRestClient")
    public RestClient notificationsRestClient(
            @Qualifier("loadBalancedRestClientBuilder") RestClient.Builder lb,
            OAuth2AuthorizedClientManager manager
    ) {
        return lb.baseUrl("lb://notifications-service")
                .requestInterceptor(new OAuth2ClientCredentialsInterceptor(manager, "transfer-service"))
                .build();
    }
}

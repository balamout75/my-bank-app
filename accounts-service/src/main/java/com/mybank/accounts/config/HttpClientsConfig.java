package com.mybank.accounts.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.client.RestClient;

@Configuration
public class HttpClientsConfig {

    @Bean
    @Primary
    public RestClient.Builder restClientBuilder() {
        // Обычный builder (для http:// и инфраструктурных вещей)
        return RestClient.builder();
    }

    @Bean
    @LoadBalanced
    public RestClient.Builder loadBalancedRestClientBuilder() {
        // LB builder (только для lb://service-id)
        return RestClient.builder();
    }

    @Bean
    public NotificationsRestClient notificationsRestClient(
            @Qualifier("loadBalancedRestClientBuilder") RestClient.Builder lbBuilder
    ) {
        RestClient client = lbBuilder.baseUrl("lb://notifications-service").build();
        return new NotificationsRestClient(client);
    }

}

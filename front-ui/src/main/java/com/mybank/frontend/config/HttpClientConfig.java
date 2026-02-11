package com.mybank.frontend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class HttpClientConfig {

    @Bean
    RestClient gatewayRestClient(@Value("${gateway.url:http://localhost:8090}") String gatewayUrl) {
        return RestClient.builder()
                .baseUrl(gatewayUrl)
                .build();
    }
}

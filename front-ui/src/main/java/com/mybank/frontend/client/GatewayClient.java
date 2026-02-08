
package com.mybank.frontend.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Component
public class GatewayClient {

    private final OAuth2AuthorizedClientService clientService;
    private final RestClient rest;

    public GatewayClient(@Value("${gateway.url:http://localhost:8090}") String gatewayUrl, OAuth2AuthorizedClientService clientService) {
        this.clientService = clientService;
        this.rest = RestClient.builder()
                .baseUrl(gatewayUrl)
                .build();
    }

}

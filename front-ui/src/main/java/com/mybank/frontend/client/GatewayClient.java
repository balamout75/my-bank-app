
package com.mybank.frontend.client;

import com.mybank.frontend.dto.client.AccountUpdateRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import com.mybank.frontend.client.dto.*;

import java.util.List;
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

    public void transfer(String toUsername, int amount) {
        rest.post()
                .uri("/api/transfer")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("toUsername", toUsername, "amount", amount))
                .retrieve()
                .toBodilessEntity();
    }
}


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

    public AccountMeResponse getMe(OAuth2AuthenticationToken authentication) {
        OAuth2AuthorizedClient client =
                clientService.loadAuthorizedClient(authentication.getAuthorizedClientRegistrationId(),
                        authentication.getName());

        String token = client.getAccessToken().getTokenValue();
        return rest.get()
                .uri("/api/accounts/me")
                .header("Authorization", "Bearer " + token)
                .retrieve()
                .body(AccountMeResponse.class);
    }

    public void updateMe(AccountUpdateRequest req, OAuth2AuthenticationToken authentication) {
        OAuth2AuthorizedClient client =
                clientService.loadAuthorizedClient(authentication.getAuthorizedClientRegistrationId(),
                        authentication.getName());

        String token = client.getAccessToken().getTokenValue();
        rest.put()
                .uri("/api/accounts/me")
                .contentType(MediaType.APPLICATION_JSON)
                .body(req)
                .header("Authorization", "Bearer " + token)
                .retrieve()
                .toBodilessEntity();
    }

    public List<AccountSummaryResponse> getAllAccounts(OAuth2AuthenticationToken authentication) {
        OAuth2AuthorizedClient client =
                clientService.loadAuthorizedClient(authentication.getAuthorizedClientRegistrationId(),
                        authentication.getName());

        String token = client.getAccessToken().getTokenValue();
        AccountSummaryResponse[] arr = rest.get()
                .uri("/api/accounts/all")
                .header("Authorization", "Bearer " + token)
                .retrieve()
                .body(AccountSummaryResponse[].class);
        return arr == null ? List.of() : List.of(arr);
    }

    public void deposit(int amount) {
        rest.post()
                .uri("/api/cash/deposit")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("amount", amount))
                .retrieve()
                .toBodilessEntity();
    }

    public void withdraw(int amount) {
        rest.post()
                .uri("/api/cash/withdraw")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("amount", amount))
                .retrieve()
                .toBodilessEntity();
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

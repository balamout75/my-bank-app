package com.mybank.frontend.client;

import com.mybank.frontend.dto.client.AccountDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

@Component
@RequiredArgsConstructor
public class AccountsClient {

    private final RestClient restClient; // настроен baseUrl = gateway

    public List<AccountDto> getAll(String accessToken) {
        return restClient.get()
                .uri("/api/accounts/all")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .retrieve()
                .body(new org.springframework.core.ParameterizedTypeReference<List<AccountDto>>() {});
    }

    public AccountDto me(String accessToken) {
        return restClient.get()
                .uri("/api/accounts/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .retrieve()
                .body(AccountDto.class);
    }
}

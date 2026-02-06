package com.mybank.frontend.client;

import com.mybank.frontend.client.dto.AccountMeResponse;
import com.mybank.frontend.client.dto.AccountSummaryResponse;
import com.mybank.frontend.client.dto.UpdateResult;
import com.mybank.frontend.dto.client.AccountUpdateRequest;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.http.HttpStatus;

import java.util.List;

@Component
@RequiredArgsConstructor
public class AccountsClient {

    private static final String ACCOUNTS_READ = "accounts-read";
    private final RestClient restClient; // настроен baseUrl = gateway

    @Retry(name = ACCOUNTS_READ)
    @CircuitBreaker(name = ACCOUNTS_READ)
    public List<AccountSummaryResponse> getAll(String accessToken) {
        return restClient.get()
                .uri("/api/accounts/all")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .retrieve()
                .body(new org.springframework.core.ParameterizedTypeReference<List<AccountSummaryResponse>>() {});
    }

    @Retry(name = ACCOUNTS_READ)
    @CircuitBreaker(name = ACCOUNTS_READ)
    public AccountMeResponse getMe(String accessToken) {
        return restClient.get()
                .uri("/api/accounts/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .retrieve()
                .body(AccountMeResponse.class);
    }

    public void updateMe(AccountUpdateRequest req, String accessToken) {
        restClient.put()
                .uri("/api/accounts/me")
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .body(req)
                .retrieve()
                .toBodilessEntity();
    }
}

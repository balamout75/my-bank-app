package com.mybank.frontend.client;

import com.mybank.frontend.dto.client.CashOperationDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@RequiredArgsConstructor
public class CashClient {

    private final RestClient restClient; // baseUrl = gateway

    public void deposit(String accessToken, CashOperationDto dto) {
        restClient.post()
                .uri("/api/cash/deposit")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .body(dto)
                .retrieve()
                .toBodilessEntity();
    }

    public void withdraw(String accessToken, CashOperationDto dto) {
        restClient.post()
                .uri("/api/cash/withdraw")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .body(dto)
                .retrieve()
                .toBodilessEntity();
    }
}
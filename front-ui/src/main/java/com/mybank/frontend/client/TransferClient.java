package com.mybank.frontend.client;

import com.mybank.frontend.dto.client.TransferDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@RequiredArgsConstructor
public class TransferClient {

    private final RestClient restClient;

    public void transfer(String accessToken, TransferDto dto) {
        restClient.post()
                .uri("/api/transfer")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .body(dto)
                .retrieve()
                .toBodilessEntity();
    }
}
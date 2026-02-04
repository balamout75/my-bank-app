package com.mybank.frontend.client;

import com.mybank.frontend.dto.client.CashOperationRequest;
import com.mybank.frontend.dto.client.OperationKeyResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@RequiredArgsConstructor
public class CashClient {

    private static final String SERVICE_WRITE = "business-service-write";
    private static final String SERVICE_READ = "business-service-read";
    private final RestClient restClient; // baseUrl = gateway

    @Retry(name = SERVICE_READ)
    @CircuitBreaker(name = SERVICE_READ)
    public OperationKeyResponse getOperationKey(String accessToken) {
        return restClient.get()
                .uri("/api/cash/operation-key")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .retrieve()
                .body(OperationKeyResponse.class);
    }

    @Retry(name = SERVICE_WRITE)
    @CircuitBreaker(name = SERVICE_WRITE)
    public void operate(String accessToken, CashOperationRequest dto) {
        restClient.post()
                .uri("/api/cash/operate")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .body(dto)
                .retrieve()
                .toBodilessEntity();
    }

}
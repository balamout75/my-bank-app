package com.mybank.frontend.client;

import com.mybank.frontend.dto.client.TransferOperationRequest;
import tools.jackson.databind.ObjectMapper;
import com.mybank.frontend.dto.client.CashOperationRequest;
import com.mybank.frontend.dto.client.OperationKeyResponse;
import com.mybank.frontend.exception.InsufficientFundsClientException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.ProblemDetail;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
public class TransferClient {

    private static final String SERVICE_WRITE = "business-service-write";
    private static final String SERVICE_READ = "business-service-read";
    private final RestClient restClient; // baseUrl = gateway
    private final ObjectMapper objectMapper;

    @Retry(name = SERVICE_READ)
    @CircuitBreaker(name = SERVICE_READ)
    public OperationKeyResponse getOperationKey(String accessToken) {
        return restClient.get()
                .uri("/api/transfer/operation-key")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .retrieve()
                .body(OperationKeyResponse.class);
    }

    @Retry(name = SERVICE_WRITE)
    @CircuitBreaker(name = SERVICE_WRITE)
    public void transfer(String accessToken, TransferOperationRequest dto) {
        restClient.post()
                .uri("/api/transfer/transfer")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .body(dto)
                .retrieve()
                .onStatus(status -> status.value() == 422, this::handleInsufficientFunds)
                .toBodilessEntity();
    }

    private void handleInsufficientFunds(HttpRequest req, ClientHttpResponse res) throws IOException {
        ProblemDetail pd = objectMapper.readValue(res.getBody(), ProblemDetail.class);
        throw new InsufficientFundsClientException(
                pd.getDetail(),
                toBigDecimal(pd.getProperties().get("currentBalance")),
                toBigDecimal(pd.getProperties().get("requestedAmount"))
        );
    }

    private BigDecimal toBigDecimal(Object v) {
        if (v == null) return null;
        if (v instanceof BigDecimal bd) return bd;
        return new BigDecimal(String.valueOf(v));
    }

}
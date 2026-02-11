package com.mybank.cash.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mybank.cash.dto.BalanceUpdateRequest;
import com.mybank.cash.exception.InsufficientFundsException;
import com.mybank.cash.exception.ServiceUnavailableException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;

@Component
public class AccountsClient {

    private static final Logger log = LoggerFactory.getLogger(AccountsClient.class);
    private static final String SERVICE_NAME = "accounts-service";

    private final RestClient accountsRestClient;
    private final ObjectMapper objectMapper;

    public AccountsClient(@Qualifier("accountsRestClient") RestClient accountsRestClient, ObjectMapper objectMapper) {
        this.accountsRestClient = accountsRestClient;
        this.objectMapper = objectMapper;
    }

    @CircuitBreaker(name = SERVICE_NAME)
    @Retry(name = SERVICE_NAME, fallbackMethod = "updateBalanceFallback")
    public void updateBalance(BalanceUpdateRequest request) {
        log.debug("Обновление баланса: username={}, amount={}, type={}",
                request.username(), request.amount(), request.cashOperationType());

        accountsRestClient.post()
                .uri("/accounts/balance")
                .body(request)
                .retrieve()
                .toBodilessEntity();

        log.info("Баланс успешно обновлён для пользователя: {}", request.username());
    }

    private void updateBalanceFallback(BalanceUpdateRequest request, Exception e) {
        // Бизнес-ошибки (4xx) — пробрасываем клиенту, не маскируем
        if (e instanceof HttpClientErrorException clientError) {
            if (clientError.getStatusCode().value() == 422) {
                throw parseInsufficientFunds(clientError);
            }
            // Другие 4xx — тоже пробрасываем как есть
            log.warn("Бизнес-ошибка от accounts-service: {}", clientError.getMessage());
            throw clientError;
        }

        // Технические ошибки (5xx, timeout, connection refused) — fallback
        log.error("accounts-service недоступен. Username: {}, Amount: {}, Type: {}, Error: {}",
                request.username(), request.amount(), request.cashOperationType(), e.getMessage());

        throw new ServiceUnavailableException(
                "Сервис аккаунтов временно недоступен. Попробуйте позже."
        );
    }

    private InsufficientFundsException parseInsufficientFunds(HttpClientErrorException e) {
        try {
            JsonNode json = objectMapper.readTree(e.getResponseBodyAsString());
            BigDecimal currentBalance = json.has("currentBalance")
                    ? json.get("currentBalance").decimalValue()
                    : BigDecimal.ZERO;
            BigDecimal requestedAmount = json.has("requestedAmount")
                    ? json.get("requestedAmount").decimalValue()
                    : BigDecimal.ZERO;
            String detail = json.has("detail")
                    ? json.get("detail").asText()
                    : "Недостаточно средств";

            return new InsufficientFundsException(detail, currentBalance, requestedAmount);
        } catch (Exception ex) {
            return new InsufficientFundsException("Недостаточно средств", BigDecimal.ZERO, BigDecimal.ZERO);
        }
    }

}

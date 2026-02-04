package com.mybank.cash.client;

import com.mybank.cash.dto.BalanceUpdateRequest;
import com.mybank.cash.exception.ServiceUnavailableException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class AccountsClient {

    private static final Logger log = LoggerFactory.getLogger(AccountsClient.class);
    private static final String SERVICE_NAME = "accounts-service";

    private final RestClient accountsRestClient;

    public AccountsClient(@Qualifier("accountsRestClient") RestClient accountsRestClient) {
        this.accountsRestClient = accountsRestClient;
    }

    @CircuitBreaker(name = SERVICE_NAME)
    @Retry(name = SERVICE_NAME, fallbackMethod = "updateBalanceFallback")
    public void updateBalance(BalanceUpdateRequest request) {
        log.debug("Обновление баланса: username={}, amount={}, type={}",
                request.username(), request.amount(), request.operationType());

        accountsRestClient.post()
                .uri("/accounts/balance")
                .body(request)
                .retrieve()
                .toBodilessEntity();

        log.info("Баланс успешно обновлён для пользователя: {}", request.username());
    }

    /**
     * Fallback метода Retry — вызывается когда:
     * 1) исчерпаны все retry-попытки
     * 2) выброшено исключение, которое Retry не ретраит (например, circuit OPEN -> CallNotPermittedException)
     */
    private void updateBalanceFallback(BalanceUpdateRequest request, Exception e) {
        log.error("accounts-service недоступен. Username: {}, Amount: {}, Type: {}, Error: {}",
                request.username(), request.amount(), request.operationType(), e.getMessage());

        throw new ServiceUnavailableException(
                "Сервис аккаунтов временно недоступен. Попробуйте позже."
        );
    }
}

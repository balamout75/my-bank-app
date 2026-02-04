package com.mybank.cash.client;

import com.mybank.cash.dto.NotificationRequest;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class NotificationsClient {

    private static final Logger log = LoggerFactory.getLogger(NotificationsClient.class);
    private static final String SERVICE_NAME = "notifications-service";

    private final RestClient notificationsRestClient;

    public NotificationsClient(@Qualifier("notificationsRestClient") RestClient notificationsRestClient) {
        this.notificationsRestClient = notificationsRestClient;
    }

    @CircuitBreaker(name = SERVICE_NAME, fallbackMethod = "sendFallback")
    @Retry(name = SERVICE_NAME)
    public void send(NotificationRequest request) {
        log.debug("Отправка уведомления: type={}, username={}", 
                request.type(), request.username());

        notificationsRestClient.post()
                .uri("/notifications")
                .body(request)
                .retrieve()
                .toBodilessEntity();

        log.info("Уведомление успешно отправлено: type={}", request.type());
    }

    /**
     * Fallback метод — просто логируем, не бросаем исключение.
     * Уведомления не критичны для основной операции.
     */
    private void sendFallback(NotificationRequest request, Exception e) {
        log.warn("notifications-service недоступен. Уведомление не отправлено: type={}, username={}, error={}",
                request.type(), request.username(), e.getMessage());
        
        // Не бросаем исключение — уведомления не критичны
        // TODO: можно сохранить в очередь для повторной отправки позже
    }
}

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
    public boolean send(NotificationRequest request) {
        log.debug("🚀 cash→notifications: type={} username={} opId={}",
                request.type(), request.username(), request.operationId());

        notificationsRestClient.post()
                .uri("/notifications")
                .body(request)
                .retrieve()
                .toBodilessEntity();

        log.info("🚀✅ notifications accepted: opId={} type={}", request.operationId(), request.type());
        return true;
    }

    private boolean sendFallback(NotificationRequest request, Exception e) {
        log.warn("🚀⚠️ notifications unavailable: opId={} type={} user={} error={}",
                request.operationId(), request.type(), request.username(), e.getMessage());
        return false;
    }
}
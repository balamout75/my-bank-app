package com.mybank.accounts.client.notifications;

import com.mybank.accounts.client.notifications.dto.NotificationRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@RequiredArgsConstructor
public class NotificationsClient {

    private final RestClient.Builder restClientBuilder;

    public void send(NotificationRequest req) {
        // Важно: межсервисное общение напрямую, через Service Discovery
        RestClient client = restClientBuilder.baseUrl("lb://notifications-service").build();

        client.post()
                .uri("/notifications")
                .body(req)
                .retrieve()
                .toBodilessEntity();
    }
}

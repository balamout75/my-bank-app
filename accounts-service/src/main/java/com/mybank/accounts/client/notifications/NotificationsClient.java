package com.mybank.accounts.client.notifications;

import com.mybank.accounts.client.notifications.dto.NotificationRequest;
import com.mybank.accounts.config.NotificationsRestClient;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class NotificationsClient {

    private final RestClient client;

    public NotificationsClient(NotificationsRestClient notificationsRestClient) {
        this.client = notificationsRestClient.client();
    }

    public void send(NotificationRequest req) {
        // Важно: межсервисное общение напрямую, через Service Discovery
        client.post()
                .uri("/notifications")
                .body(req)
                .retrieve()
                .toBodilessEntity();
    }
}

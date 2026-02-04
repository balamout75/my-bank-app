package com.mybank.accounts.client.notifications;

import com.mybank.accounts.client.notifications.dto.NotificationRequest;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class NotificationsClient {

    private final RestClient notificationsRestClient;

    public NotificationsClient(@Qualifier("notificationsRestClient") RestClient notificationsRestClient) {
        this.notificationsRestClient = notificationsRestClient;
    }

    public void send(NotificationRequest req) {
        // Важно: межсервисное общение напрямую, через Service Discovery
        notificationsRestClient.post()
                .uri("/notifications")
                .body(req)
                .retrieve()
                .toBodilessEntity();
    }
}

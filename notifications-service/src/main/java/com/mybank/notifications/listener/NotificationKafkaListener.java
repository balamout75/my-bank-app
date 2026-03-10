package com.mybank.notifications.listener;

import com.mybank.notifications.dto.NotificationEvent;
import com.mybank.notifications.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationKafkaListener {

    private final NotificationService notificationService;

    @KafkaListener(
            topics = "${application.kafka.topic.notifications:notifications}",
            groupId = "${spring.kafka.consumer.group-id:notifications-service}"
    )
    public void onNotificationEvent(NotificationEvent event) {
        log.info("📩 KAFKA RECEIVED: service={}, opId={}, user={}",
                event.service(), event.operationId(), event.username());
        try {
            boolean created = notificationService.createFromEvent(event);
            if (created) {
                log.info("✅ NOTIFICATION CREATED: service={}, opId={}, user={}",
                        event.service(), event.operationId(), event.username());
            } else {
                log.info("⏭️ NOTIFICATION SKIPPED (duplicate): service={}, opId={}",
                        event.service(), event.operationId());
            }
        } catch (Exception e) {
            log.error("❌ NOTIFICATION ERROR: service={}, opId={}, error={}",
                    event.service(), event.operationId(), e.getMessage(), e);
            throw e;
        }
    }
}

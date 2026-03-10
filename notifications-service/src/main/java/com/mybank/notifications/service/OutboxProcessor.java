package com.mybank.notifications.service;

import com.mybank.notifications.model.Notification;
import com.mybank.notifications.model.OperationStatus;
import com.mybank.notifications.repository.NotificationRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@Slf4j
public class OutboxProcessor {

    private final NotificationRepository notificationRepository;
    private final MeterRegistry meterRegistry;

    @Value("${application.outbox.order.limit:10}")
    private int limit;

    @Value("${application.outbox.max-attempts:5}")
    private int maxAttempts;

    public OutboxProcessor(NotificationRepository notificationRepository,
                           MeterRegistry meterRegistry) {
        this.notificationRepository = notificationRepository;
        this.meterRegistry = meterRegistry;
    }

    @Scheduled(fixedDelayString = "${application.outbox.fixed-delay-ms:5000}")
    public void process() {
        var page = notificationRepository.findByStatus(
                OperationStatus.UPDATED,
                Pageable.ofSize(limit)
        );
        page.forEach(this::sendNotification);
    }

    public void sendNotification(Notification notification) {
        try {
            Map<String, Object> payload = notification.getPayload() != null ? notification.getPayload() : Map.of();
            log.info("🚀✅ NOTIFIED opId={} user={} service={} payload={}",
                    notification.getId().getOperationId(),
                    notification.getUsername(),
                    notification.getId().getService(),
                    notification.getPayload());
            notification.setStatus(OperationStatus.NOTIFIED);
        } catch (Exception e) { // такого случиться не может, тем не менее
            if (notification.getAttempts() < maxAttempts) {
                log.info("🚀⚠️ RETRY opId={} user={} service={} attempt={} payload={}",
                        notification.getId().getOperationId(),
                        notification.getUsername(),
                        notification.getId().getService(),
                        notification.getAttempts(),
                        notification.getPayload());
                notification.setAttempts(notification.getAttempts() + 1);
                notification.setError("notification unavailable; will retry later");

            } else {
                log.error("🚀💥 NOTIFICATION FAILED opId={} user={} service={} attempt={} payload={}",
                        notification.getId().getOperationId(),
                        notification.getUsername(),
                        notification.getId().getService(),
                        notification.getAttempts(),
                        notification.getPayload());
                notification.setStatus(OperationStatus.UNNOTIFIED);
                notification.setError("notification unavailable");

                Counter.builder("notification_failed_total")
                        .description("Number of failed notification attempts")
                        .tag("username", notification.getUsername())
                        .register(meterRegistry)
                        .increment();
            }
        }
        notification.touch();
        notificationRepository.save(notification);

    }
}
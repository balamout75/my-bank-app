package com.mybank.notifications.service;

import com.mybank.notifications.model.OutboxEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Учебная реализация: "доставка" == лог.
 * В реальном проекте здесь был бы NATS/Kafka/Rabbit/SMTP и т.д.
 */
@Component
@Slf4j
public class LoggingOutboxEventPublisher implements OutboxEventPublisher {
    @Override
    public void publish(OutboxEvent event) {
        log.info("OUTBOX PUBLISH eventId={} opId={} type={} payload={}",
                event.getId(), event.getOperationId(), event.getEventType(), event.getPayload());
    }
}

package com.mybank.notifications.service;

import com.mybank.notifications.model.OutboxEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class LoggingOutboxEventPublisher implements OutboxEventPublisher {
    @Override
    public void publish(OutboxEvent event) {
        String payloadPreview = preview(event.getPayload());
        log.info("🚀➡️📨 OUTBOX PUBLISH | eventId={} opId={} type={} payload={}",
                event.getId(),
                event.getOperationId(),
                event.getEventType(),
                event.getPayload());
    }
    private String preview(Object payload) {
        if (payload == null) return "null";
        String s = payload.toString().replaceAll("\\s+", " ");
        int max = 200;
        return s.length() > max
                ? s.substring(0, max) + "...(truncated)"
                : s;
    }
}

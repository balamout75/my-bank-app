package com.mybank.notifications.service;

import com.mybank.notifications.dto.NotificationRequest;
import com.mybank.notifications.model.Notification;
import com.mybank.notifications.model.NotificationStatus;
import com.mybank.notifications.model.OutboxEvent;
import com.mybank.notifications.model.OutboxStatus;
import com.mybank.notifications.repository.NotificationRepository;
import com.mybank.notifications.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class NotificationCommandService {

    private static final String AGGREGATE_TYPE = "Notification";
    private static final String EVENT_TYPE_SEND = "SEND";

    private final NotificationRepository notificationRepository;
    private final OutboxEventRepository outboxEventRepository;

    /**
     * Идемпотентное создание:
     * - operationId уникален
     * - notification + outbox записываются в одной транзакции
     */
    @Transactional
    public Notification createAndEnqueue(NotificationRequest req) {
        try {
            Notification n = new Notification();
            n.setOperationId(req.operationId());
            n.setType(req.type());
            n.setUsername(req.username());
            n.setMessage(req.message());
            n.setMeta(req.meta());
            n.setStatus(NotificationStatus.ACCEPTED);
            n.setCreatedAt(LocalDateTime.now());
            n = notificationRepository.save(n);

            OutboxEvent e = new OutboxEvent();
            e.setOperationId(req.operationId());
            e.setAggregateType(AGGREGATE_TYPE);
            e.setAggregateId(n.getId());
            e.setEventType(EVENT_TYPE_SEND);
            e.setPayload(buildPayload(n));
            e.setStatus(OutboxStatus.NEW);
            e.setAttempts(0);
            e.setNextAttemptAt(LocalDateTime.now());
            e.setCreatedAt(LocalDateTime.now());
            outboxEventRepository.save(e);
            return n;

        } catch (DataIntegrityViolationException ex) {
            // duplicate operationId -> возвращаем существующую запись (идемпотентность)
            return notificationRepository.findByOperationId(req.operationId())
                    .orElseThrow(() -> ex);
        }
    }

    private Map<String, Object> buildPayload(Notification n) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("notificationId", n.getId());
        payload.put("operationId", n.getOperationId());
        payload.put("type", n.getType());
        payload.put("username", n.getUsername());
        payload.put("message", n.getMessage());
        payload.put("meta", n.getMeta() == null ? Map.of() : n.getMeta());
        return payload;
    }
}

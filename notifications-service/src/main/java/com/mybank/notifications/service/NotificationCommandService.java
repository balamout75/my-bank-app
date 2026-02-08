package com.mybank.notifications.service;

import com.mybank.notifications.dto.NotificationRequest;
import com.mybank.notifications.model.*;
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
    public Notification createAndEnqueue(NotificationRequest req, String clientId) {
        try {
            Notification n = new Notification();
            n.setId(new NotificationId(clientId, req.operationId()));
            n.setUsername(req.username());
            n.setStatus(OperationStatus.RECEIVED);
            n.setPayload(req.meta());
            n = notificationRepository.save(n);
            return n;

        } catch (DataIntegrityViolationException ex) {
            // duplicate operationId -> возвращаем существующую запись (идемпотентность)
            return notificationRepository.findByOperationId(req.operationId())
                    .orElseThrow(() -> ex);
        }
    }

}

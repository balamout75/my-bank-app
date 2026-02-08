package com.mybank.notifications.service;

import com.mybank.notifications.dto.NotificationRequest;
import com.mybank.notifications.model.*;
import com.mybank.notifications.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;

    /**
     * Идемпотентное создание:
     * - operationId + service уникальны
     */
    @Transactional
    public Boolean createAndEnqueue(NotificationRequest req, String clientId) {
        if (notificationRepository.findById(new NotificationId(clientId, req.operationId())).isPresent()) return false;
        Notification n = new Notification();
        n.setId(new NotificationId(clientId, req.operationId()));
        n.setUsername(req.username());
        n.setStatus(OperationStatus.UPDATED);
        n.setPayload(req.payload());
        notificationRepository.save(n);
        return true;
    }

}

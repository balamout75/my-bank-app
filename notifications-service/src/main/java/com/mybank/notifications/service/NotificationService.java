package com.mybank.notifications.service;

import com.mybank.notifications.dto.NotificationEvent;
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
     * Создание уведомления из Kafka-события.
     * Идемпотентно: operationId + service уникальны.
     *
     * @return true если уведомление создано, false если дубликат
     */
    @Transactional
    public boolean createFromEvent(NotificationEvent event) {
        NotificationId id = new NotificationId(event.service(), event.operationId());

        if (notificationRepository.findById(id).isPresent()) {
            return false;
        }

        Notification n = new Notification();
        n.setId(id);
        n.setUsername(event.username());
        n.setStatus(OperationStatus.UPDATED);
        n.setPayload(event.payload());
        notificationRepository.save(n);
        return true;
    }

}

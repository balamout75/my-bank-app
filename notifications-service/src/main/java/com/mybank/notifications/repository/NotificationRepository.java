package com.mybank.notifications.repository;

import com.mybank.notifications.model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    Optional<Notification> findByOperationId(Long operationId);
}

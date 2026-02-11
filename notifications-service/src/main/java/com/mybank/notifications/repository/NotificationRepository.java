package com.mybank.notifications.repository;

import com.mybank.notifications.model.Notification;
import com.mybank.notifications.model.NotificationId;
import com.mybank.notifications.model.OperationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, NotificationId> {

    Optional<Notification> findById(NotificationId id);

    Page<Notification> findByStatus(OperationStatus operationStatus, Pageable pageable);
}

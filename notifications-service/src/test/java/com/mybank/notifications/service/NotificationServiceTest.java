package com.mybank.notifications.service;

import com.mybank.notifications.dto.NotificationEvent;
import com.mybank.notifications.model.Notification;
import com.mybank.notifications.model.NotificationId;
import com.mybank.notifications.model.OperationStatus;
import com.mybank.notifications.repository.NotificationRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock NotificationRepository notificationRepository;

    @InjectMocks NotificationService notificationService;

    private NotificationEvent event() {
        return new NotificationEvent(
                "cash-service",
                1001L,
                "alice",
                Map.of("operation", "DEPOSIT", "amount", "500.00")
        );
    }

    // createFromEvent → новое событие → сохраняет уведомление, возвращает true	
    @Test
    void createFromEvent_new_shouldSaveAndReturnTrue() {
        var event = event();
        when(notificationRepository.findById(any(NotificationId.class))).thenReturn(Optional.empty());
        when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> inv.getArgument(0));

        boolean created = notificationService.createFromEvent(event);

        assertThat(created).isTrue();

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());

        Notification saved = captor.getValue();
        assertThat(saved.getId().getService()).isEqualTo("cash-service");
        assertThat(saved.getId().getOperationId()).isEqualTo(1001L);
        assertThat(saved.getUsername()).isEqualTo("alice");
        assertThat(saved.getStatus()).isEqualTo(OperationStatus.UPDATED);
        assertThat(saved.getPayload()).containsEntry("operation", "DEPOSIT");
    }

    // createFromEvent → дубликат → не сохраняет, возвращает false
    @Test
    void createFromEvent_duplicate_shouldReturnFalse() {
        var event = event();
        var existing = new Notification();
        existing.setId(new NotificationId("cash-service", 1001L));
        when(notificationRepository.findById(any(NotificationId.class))).thenReturn(Optional.of(existing));

        boolean created = notificationService.createFromEvent(event);

        assertThat(created).isFalse();
        verify(notificationRepository, never()).save(any());
    }
}

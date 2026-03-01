package com.mybank.notifications.service;

import com.mybank.notifications.model.Notification;
import com.mybank.notifications.model.NotificationId;
import com.mybank.notifications.model.OperationStatus;
import com.mybank.notifications.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutboxProcessorTest {

    @Mock NotificationRepository notificationRepository;

    @InjectMocks OutboxProcessor outboxProcessor;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(outboxProcessor, "limit", 10);
        ReflectionTestUtils.setField(outboxProcessor, "maxAttempts", 5);
    }

    private Notification notification(String service, Long opId, String username) {
        var n = new Notification();
        n.setId(new NotificationId(service, opId));
        n.setUsername(username);
        n.setStatus(OperationStatus.UPDATED);
        n.setPayload(Map.of("operation", "DEPOSIT"));
        n.setAttempts(0);
        n.setCreatedAt(LocalDateTime.now());
        n.setUpdatedAt(LocalDateTime.now());
        return n;
    }

    //sendNotification → статус меняется на NOTIFIED
    @Test
    void sendNotification_shouldSetStatusNotified() {
        var n = notification("cash-service", 1L, "alice");

        outboxProcessor.sendNotification(n);

        assertThat(n.getStatus()).isEqualTo(OperationStatus.NOTIFIED);
        verify(notificationRepository).save(n);
    }

    //process → подбирает записи со статусом UPDATED и обрабатывает
    @Test
    void process_shouldFetchAndSendNotifications() {
        var n1 = notification("cash-service", 1L, "alice");
        var n2 = notification("transfer-service", 2L, "bob");
        var page = new PageImpl<>(List.of(n1, n2));

        when(notificationRepository.findByStatus(eq(OperationStatus.UPDATED), any(Pageable.class)))
                .thenReturn(page);

        outboxProcessor.process();

        assertThat(n1.getStatus()).isEqualTo(OperationStatus.NOTIFIED);
        assertThat(n2.getStatus()).isEqualTo(OperationStatus.NOTIFIED);
        verify(notificationRepository).save(n1);
        verify(notificationRepository).save(n2);
    }
}

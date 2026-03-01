package com.mybank.notifications.listener;

import com.mybank.notifications.dto.NotificationEvent;
import com.mybank.notifications.service.NotificationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationKafkaListenerTest {

    @Mock NotificationService notificationService;

    @InjectMocks NotificationKafkaListener listener;

    private NotificationEvent event() {
        return new NotificationEvent(
                "cash-service", 1001L, "alice",
                Map.of("operation", "DEPOSIT", "amount", "500.00")
        );
    }

    @Test
    @DisplayName("onNotificationEvent → новое событие → вызывает createFromEvent")
    void onEvent_new_shouldCallService() {
        var event = event();
        when(notificationService.createFromEvent(event)).thenReturn(true);

        listener.onNotificationEvent(event);

        verify(notificationService).createFromEvent(event);
    }

    @Test
    @DisplayName("onNotificationEvent → дубликат → вызывает createFromEvent, не падает")
    void onEvent_duplicate_shouldNotThrow() {
        var event = event();
        when(notificationService.createFromEvent(event)).thenReturn(false);

        listener.onNotificationEvent(event);

        verify(notificationService).createFromEvent(event);
    }

    @Test
    @DisplayName("onNotificationEvent → ошибка сервиса → пробрасывает исключение для Kafka retry")
    void onEvent_error_shouldRethrow() {
        var event = event();
        when(notificationService.createFromEvent(event)).thenThrow(new RuntimeException("DB down"));

        assertThatThrownBy(() -> listener.onNotificationEvent(event))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("DB down");
    }
}

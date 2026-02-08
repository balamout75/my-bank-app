package com.mybank.transfer.outbox;

import com.mybank.transfer.client.NotificationsClient;
import com.mybank.transfer.dto.NotificationRequest;
import com.mybank.transfer.dto.OperationStatus;
import com.mybank.transfer.model.CashOperation;
import com.mybank.transfer.repository.CashOperationRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@Slf4j
public class OutboxProcessor {

    private final CashOperationRepository cashOperationRepository;
    private final NotificationsClient notificationsClient;
    @Value("${application.outbox.order.limit:10}")
    private int limit;

    @Value("${application.outbox.max-attempts:5}")
    private int maxAttempts;

    public OutboxProcessor(CashOperationRepository cashOperationRepository, NotificationsClient notificationsClient) {
        this.cashOperationRepository = cashOperationRepository;
        this.notificationsClient = notificationsClient;
    }

    @Scheduled(fixedDelayString = "${application.outbox.fixed-delay-ms:5000}")
    public void process() {
        var page = cashOperationRepository.findByStatus(
                OperationStatus.UPDATED,
                Pageable.ofSize(limit)
        );
        page.forEach(this::sendNotification);
    }

    public void sendNotification(CashOperation op) {
        boolean sent = notificationsClient.send(new NotificationRequest(
                op.getOperationId(),
                op.getType().toString(),
                op.getUsername(),
                op.getType().toString().toLowerCase() + " completed",
                Map.of("amount", op.getAmount())
        ));

        if (sent) {
            op.setStatus(OperationStatus.NOTIFIED);
            log.info("🚀✅ NOTIFIED opId={} user={}", op.getOperationId(), op.getUsername());
        } else {
            if (op.getNotificationAttempts() < maxAttempts) {
                log.warn("🚀⚠️ RETRY opId={} user={} attempt={}", op.getOperationId(), op.getUsername(), op.getNotificationAttempts());
                op.setNotificationAttempts(op.getNotificationAttempts() + 1);
                op.setNotificationError("notifications-service unavailable; will retry later");

            } else {
                log.error("🚀💥 NOTIFICATION FAILED opId={} user={} attempts={}", op.getOperationId(), op.getUsername(), op.getNotificationAttempts());
                op.setStatus(OperationStatus.UNNOTIFIED);
                op.setNotificationError("notifications-service unavailable");
            }
        }
        op.touch();
        cashOperationRepository.save(op);

    }
}

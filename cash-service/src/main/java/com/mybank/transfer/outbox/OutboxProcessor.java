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
        Map<String, Object> payload = Map.of("opreration", op.getType (), "amount", op.getAmount());
        boolean sent = notificationsClient.send(new NotificationRequest(
                op.getOperationId(),
                op.getUsername(),
                payload
        ));

        if (sent) {
            op.setStatus(OperationStatus.NOTIFIED);
            log.info("🚀✅ NOTIFIED opId={} user={} payload={}", op.getOperationId(), op.getUsername(), payload);
        } else {
            if (op.getNotificationAttempts() < maxAttempts) {
                log.warn("🚀⚠️ RETRY opId={} user={} attempt={} payload={}", op.getOperationId(), op.getUsername(), op.getNotificationAttempts(), payload);
                op.setNotificationAttempts(op.getNotificationAttempts() + 1);
                op.setNotificationError("notifications-service unavailable; will retry later");

            } else {
                log.error("🚀💥 NOTIFICATION FAILED opId={} user={} attempts={} payload={}", op.getOperationId(), op.getUsername(), op.getNotificationAttempts(), payload);
                op.setStatus(OperationStatus.UNNOTIFIED);
                op.setNotificationError("notifications-service unavailable");
            }
        }
        op.touch();
        cashOperationRepository.save(op);

    }
}

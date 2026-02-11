package com.mybank.accounts.outbox;

import com.mybank.accounts.client.notifications.NotificationsClient;
import com.mybank.accounts.dto.NotificationRequest;
import com.mybank.accounts.model.AccountOperation;
import com.mybank.accounts.model.OperationStatus;
import com.mybank.accounts.repository.AccountOperationRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@Slf4j
public class OutboxProcessor {

    private final AccountOperationRepository accountOperationRepository;
    private final NotificationsClient notificationsClient;
    @Value("${application.outbox.order.limit:10}")
    private int limit;

    @Value("${application.outbox.max-attempts:5}")
    private int maxAttempts;

    public OutboxProcessor(AccountOperationRepository accountOperationRepository, NotificationsClient notificationsClient) {
        this.accountOperationRepository = accountOperationRepository;
        this.notificationsClient = notificationsClient;
    }

    @Scheduled(fixedDelayString = "${application.outbox.fixed-delay-ms:5000}")
    public void process() {
        var page = accountOperationRepository.findByStatus(
                OperationStatus.UPDATED,
                Pageable.ofSize(limit)
        );
        page.forEach(this::sendNotification);
    }

    public void sendNotification(AccountOperation op) {
        Map<String, Object> payload = op.getPayload() != null ? op.getPayload() :Map.of();
        boolean sent = notificationsClient.send(new NotificationRequest(
                op.getOperationId(),
                op.getUsername(),
                payload
        ));

        if (sent) {
            op.setStatus(OperationStatus.NOTIFIED);
            log.info("🚀✅ NOTIFIED opId={} user={} payload={}", op.getOperationId(), op.getUsername(), op.getPayload());
        } else {
            if (op.getAttempts() < maxAttempts) {
                log.warn("🚀⚠️ RETRY opId={} user={} attempt={} payload={}", op.getOperationId(), op.getUsername(), op.getAttempts(), op.getPayload());
                op.setAttempts(op.getAttempts() + 1);
                op.setError("notifications-service unavailable; will retry later");

            } else {
                log.error("🚀💥 NOTIFICATION FAILED opId={} user={} attempts={} payload={}", op.getOperationId(), op.getUsername(), op.getAttempts(), op.getPayload());
                op.setStatus(OperationStatus.UNNOTIFIED);
                op.setError("notifications-service unavailable");
            }
        }
        op.touch();
        accountOperationRepository.save(op);

    }
}

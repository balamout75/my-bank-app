package com.mybank.accounts.outbox;

import com.mybank.accounts.dto.NotificationEvent;
import com.mybank.accounts.model.AccountOperation;
import com.mybank.accounts.model.OperationStatus;
import com.mybank.accounts.repository.AccountOperationRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@Slf4j
public class OutboxProcessor {

    private final AccountOperationRepository accountOperationRepository;
    private final KafkaTemplate<String, NotificationEvent> kafkaTemplate;

    @Value("${application.outbox.order.limit:10}")
    private int limit;

    @Value("${application.outbox.max-attempts:5}")
    private int maxAttempts;

    @Value("${application.kafka.topic.notifications:notifications}")
    private String notificationsTopic;

    public OutboxProcessor(AccountOperationRepository accountOperationRepository,
                           KafkaTemplate<String, NotificationEvent> kafkaTemplate) {
        this.accountOperationRepository = accountOperationRepository;
        this.kafkaTemplate = kafkaTemplate;
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
        Map<String, Object> payload = op.getPayload() != null ? op.getPayload() : Map.of();

        NotificationEvent event = new NotificationEvent(
                "accounts-service",
                op.getOperationId(),
                op.getUsername(),
                payload
        );
        try {
            kafkaTemplate.send(notificationsTopic, op.getOperationId().toString(), event).get();
            op.setStatus(OperationStatus.NOTIFIED);
            log.info("✅ KAFKA SENT opId={} user={} topic={}", op.getOperationId(), op.getUsername(), notificationsTopic);
        } catch (Exception e) {
            if (op.getAttempts() < maxAttempts) {
                log.warn("⚠️ KAFKA RETRY opId={} user={} attempt={} error={}",
                        op.getOperationId(), op.getUsername(), op.getAttempts(), e.getMessage());
                op.setAttempts(op.getAttempts() + 1);
                op.setError("kafka send failed; will retry later: " + e.getMessage());
            } else {
                log.error("💥 KAFKA FAILED opId={} user={} attempts={}",
                        op.getOperationId(), op.getUsername(), op.getAttempts());
                op.setStatus(OperationStatus.UNNOTIFIED);
                op.setError("kafka send failed after max attempts: " + e.getMessage());
            }
        }
        op.touch();
        accountOperationRepository.save(op);
    }
}

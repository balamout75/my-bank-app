package com.mybank.cash.outbox;

import com.mybank.cash.dto.NotificationEvent;
import com.mybank.cash.dto.OperationStatus;
import com.mybank.cash.model.CashOperation;
import com.mybank.cash.repository.CashOperationRepository;
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

    private final CashOperationRepository cashOperationRepository;
    private final KafkaTemplate<String, NotificationEvent> kafkaTemplate;

    @Value("${application.outbox.order.limit:10}")
    private int limit;

    @Value("${application.outbox.max-attempts:5}")
    private int maxAttempts;

    @Value("${application.kafka.topic.notifications:notifications}")
    private String notificationsTopic;

    public OutboxProcessor(CashOperationRepository cashOperationRepository,
                           KafkaTemplate<String, NotificationEvent> kafkaTemplate) {
        this.cashOperationRepository = cashOperationRepository;
        this.kafkaTemplate = kafkaTemplate;
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

        NotificationEvent event = new NotificationEvent(
                "cash-service",
                op.getOperationId(),
                op.getUsername(),
                payload
        );
        try {
            kafkaTemplate.send(notificationsTopic, op.getOperationId().toString(), event).get();
            op.setStatus(OperationStatus.NOTIFIED);
            log.info("✅ KAFKA SENT opId={} user={} topic={}", op.getOperationId(), op.getUsername(), notificationsTopic);
        } catch (Exception e) {
            if (op.getNotificationAttempts() < maxAttempts) {
                log.warn("⚠️ KAFKA RETRY opId={} user={} attempt={} error={}",
                        op.getOperationId(), op.getUsername(), op.getNotificationAttempts(), e.getMessage());
                op.setNotificationAttempts(op.getNotificationAttempts() + 1);
                op.setNotificationError("kafka send failed; will retry later: " + e.getMessage());
            } else {
                log.error("💥 KAFKA FAILED opId={} user={} attempts={}",
                        op.getOperationId(), op.getUsername(), op.getNotificationAttempts());
                op.setStatus(OperationStatus.UNNOTIFIED);
                op.setNotificationError("kafka send failed after max attempts: " + e.getMessage());
            }
        }
        op.touch();
        cashOperationRepository.save(op);
    }
}

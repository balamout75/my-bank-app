package com.mybank.transfer.outbox;

import com.mybank.transfer.dto.NotificationEvent;
import com.mybank.transfer.dto.OperationStatus;
import com.mybank.transfer.model.TransferOperation;
import com.mybank.transfer.repository.TransferOperationRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ExecutionException;

@Service
@Slf4j
public class OutboxProcessor {

    private final TransferOperationRepository transferOperationRepository;
    private final KafkaTemplate<String, NotificationEvent> kafkaTemplate;

    @Value("${application.outbox.order.limit:10}")
    private int limit;

    @Value("${application.outbox.max-attempts:5}")
    private int maxAttempts;

    @Value("${application.kafka.topic.notifications:notifications}")
    private String notificationsTopic;

    public OutboxProcessor(TransferOperationRepository transferOperationRepository,
                           KafkaTemplate<String, NotificationEvent> kafkaTemplate) {
        this.transferOperationRepository = transferOperationRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Scheduled(fixedDelayString = "${application.outbox.fixed-delay-ms:5000}")
    public void process() {
        var page = transferOperationRepository.findByStatus(
                OperationStatus.UPDATED,
                Pageable.ofSize(limit)
        );
        page.forEach(this::sendNotification);
    }

    public void sendNotification(TransferOperation op) {
        Map<String, Object> payload = Map.of(
                "operation", "TRANSFER",
                "recipient", op.getRecipient(),
                "amount", op.getAmount());

        NotificationEvent event = new NotificationEvent(
                "transfer-service",
                op.getOperationId(),
                op.getUsername(),
                payload
        );
        try {
            kafkaTemplate.send(notificationsTopic, op.getOperationId().toString(), event).get();
            op.setStatus(OperationStatus.NOTIFIED);
            log.info("✅ KAFKA SENT opId={} user={} topic={}", op.getOperationId(), op.getUsername(), notificationsTopic);
        } catch (InterruptedException e) {
            // Восстанавливаем флаг прерывания — иначе Scheduler не узнает
            // что поток был прерван и graceful shutdown зависнет
            Thread.currentThread().interrupt();
            log.warn("⚠️ KAFKA INTERRUPTED opId={} user={}", op.getOperationId(), op.getUsername());
            op.setNotificationAttempts(op.getNotificationAttempts() + 1);
            op.setNotificationError("kafka send interrupted: " + e.getMessage());

        } catch (ExecutionException e) {
            if (op.getNotificationAttempts() < maxAttempts) {
                log.warn("⚠️ KAFKA RETRY opId={} user={} attempt={} error={}", op.getOperationId(), op.getUsername(), op.getNotificationAttempts(), e.getMessage());
                op.setNotificationAttempts(op.getNotificationAttempts() + 1);
                op.setNotificationError("kafka send failed; will retry later: " + e.getMessage());
            } else {
                log.error("💥 KAFKA FAILED opId={} user={} attempts={}", op.getOperationId(), op.getUsername(), op.getNotificationAttempts());
                op.setStatus(OperationStatus.UNNOTIFIED);
                op.setNotificationError("kafka send failed after max attempts: " + e.getMessage());
            }
        }
        op.touch();
        transferOperationRepository.save(op);
    }
}

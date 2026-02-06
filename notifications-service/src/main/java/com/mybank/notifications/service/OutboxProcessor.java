package com.mybank.notifications.service;

import com.mybank.notifications.model.Notification;
import com.mybank.notifications.model.NotificationStatus;
import com.mybank.notifications.model.OutboxEvent;
import com.mybank.notifications.model.OutboxStatus;
import com.mybank.notifications.repository.NotificationRepository;
import com.mybank.notifications.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Периодически читает outbox и "доставляет" события.
 *
 * Важно: используем FOR UPDATE SKIP LOCKED, чтобы можно было запускать
 * несколько инстансов и не обрабатывать одно и то же событие параллельно.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxProcessor {

    private final OutboxEventRepository outboxEventRepository;
    private final NotificationRepository notificationRepository;
    private final OutboxEventPublisher publisher;

    @Value("${outbox.poll.batch-size:50}")
    private int batchSize;

    @Value("${outbox.lock-timeout-seconds:300}")
    private long lockTimeoutSeconds;

    @Value("${outbox.retry.base-delay-ms:60000}")
    private long baseDelayMs;

    @Value("${outbox.retry.max-delay-ms:60000}")
    private long maxDelayMs;

    @Value("${outbox.retry.max-attempts:5}")
    private int maxAttempts;

    private final String instanceId = UUID.randomUUID().toString();

    @Scheduled(fixedDelayString = "${outbox.poll.fixed-delay-ms:1000}")
    public void poll() {
        int processed = processOnce();
        if (processed > 0) {
            log.debug("Outbox processed {} event(s)", processed);
        }
    }

    @Transactional
    public int processOnce() {
        // 1) Подбираем пачку и блокируем
        List<OutboxEvent> events = outboxEventRepository.lockBatchForProcessing(batchSize);
        if (events.isEmpty()) return 0;

        // 2) Помечаем как IN_PROGRESS
        LocalDateTime now = LocalDateTime.now();
        for (OutboxEvent e : events) {
            e.setStatus(OutboxStatus.IN_PROGRESS);
            e.setLockedAt(now);
            e.setLockedBy(instanceId);
        }
        outboxEventRepository.saveAll(events);

        // 3) Обрабатываем
        for (OutboxEvent e : events) {
            try {
                publisher.publish(e);

                e.setStatus(OutboxStatus.PUBLISHED);
                e.setPublishedAt(LocalDateTime.now());
                e.setLastError(null);

                // Для учебки считаем, что PUBLISHED == SENT
                notificationRepository.findById(e.getAggregateId()).ifPresent(n -> {
                    n.setStatus(NotificationStatus.SENT);
                    notificationRepository.save(n);
                });
            } catch (Exception ex) {
                int attempts = e.getAttempts() + 1;
                e.setAttempts(attempts);
                e.setLastError(ex.getClass().getSimpleName() + ": " + ex.getMessage());
                e.setNextAttemptAt(backoff(attempts));
                if (attempts >= maxAttempts) {
                    e.setStatus(OutboxStatus.DEAD);
                    notificationRepository.findById(e.getAggregateId()).ifPresent(n -> {
                        n.setStatus(NotificationStatus.FAILED);
                        notificationRepository.save(n);
                    });
                    log.error("🚀💥 DEAD Outbox publish failed | eventId={} opId={} attempts={} lastError={}",
                            e.getId(), e.getOperationId(), attempts, e.getLastError());
                } else {
                    e.setStatus(OutboxStatus.RETRY);
                    e.setNextAttemptAt(backoff(attempts));
                    log.warn("🚀⚠️ RETRY Outbox publish failed | eventId={} opId={} attempt={} next={} error={}",
                            e.getId(), e.getOperationId(), attempts, e.getNextAttemptAt(), e.getLastError());
                }
            } finally {
                e.setLockedAt(null);
                e.setLockedBy(null);
            }
        }

        outboxEventRepository.saveAll(events);
        return events.size();
    }

    private LocalDateTime backoff(int attempts) {
        long factor = 1L << Math.min(attempts - 1, 10); // ограничим степень
        long delay = Math.min(maxDelayMs, baseDelayMs * factor);
        return LocalDateTime.now().plusNanos(delay * 1_000_000);
    }
}

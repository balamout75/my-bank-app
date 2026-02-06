package com.mybank.accounts.config;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class ResilienceLoggingConfig {

    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final RetryRegistry retryRegistry;

    @PostConstruct
    public void registerEventListeners() {
        // 1) Повесить на уже созданные
        circuitBreakerRegistry.getAllCircuitBreakers().forEach(this::attachCircuitBreakerLogging);
        retryRegistry.getAllRetries().forEach(this::attachRetryLogging);

        // 2) И на те, что будут созданы позже
        circuitBreakerRegistry.getEventPublisher()
                .onEntryAdded(event -> attachCircuitBreakerLogging(event.getAddedEntry()));

        retryRegistry.getEventPublisher()
                .onEntryAdded(event -> attachRetryLogging(event.getAddedEntry()));

        log.info("📊 Resilience4j listeners registered (existing + future entries)");
    }

    private void attachCircuitBreakerLogging(CircuitBreaker cb) {
        String name = cb.getName();
        cb.getEventPublisher()
                .onStateTransition(e ->
                        log.warn("🔌 CB[{}]: {} → {}", name,
                                e.getStateTransition().getFromState(),
                                e.getStateTransition().getToState()))
                .onCallNotPermitted(e ->
                        log.warn("🚫 CB[{}]: call not permitted (OPEN)", name))
                .onError(e ->
                        log.warn("❌ CB[{}]: error: {}", name, safeMsg(e.getThrowable())))
                .onSuccess(e ->
                        log.debug("✅ CB[{}]: success in {} ms", name, e.getElapsedDuration().toMillis()));
    }

    private void attachRetryLogging(Retry retry) {
        String name = retry.getName();
        retry.getEventPublisher()
                .onRetry(e ->
                        log.warn("🔄 Retry[{}]: attempt #{}, cause: {}",
                                name, e.getNumberOfRetryAttempts(), safeMsg(e.getLastThrowable())))
                .onError(e ->
                        log.warn("❌ Retry[{}]: exhausted after {} attempts", name, e.getNumberOfRetryAttempts()))
                .onSuccess(e ->
                        log.debug("✅ Retry[{}]: success after {} attempts", name, e.getNumberOfRetryAttempts()));
    }

    private String safeMsg(Throwable t) {
        if (t == null) return "-";
        String m = t.getMessage();
        return (m == null || m.isBlank()) ? t.getClass().getSimpleName() : m;
    }
}

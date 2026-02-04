package com.mybank.cash.config;

import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryRegistry;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class ResilienceLoggingConfig {

    private static final Logger log = LoggerFactory.getLogger(ResilienceLoggingConfig.class);

    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final RetryRegistry retryRegistry;

    @PostConstruct
    public void registerEventListeners() {
        // Circuit Breaker события
        circuitBreakerRegistry.getAllCircuitBreakers().forEach(cb -> {
            String name = cb.getName();
            cb.getEventPublisher()
                    .onStateTransition(event ->
                            log.warn("🔌 CircuitBreaker [{}]: {} → {}",
                                    name,
                                    event.getStateTransition().getFromState(),
                                    event.getStateTransition().getToState()))
                    .onSuccess(event ->
                            log.debug("✅ CircuitBreaker [{}]: успешный вызов за {} мс",
                                    name,
                                    event.getElapsedDuration().toMillis()))
                    .onError(event ->
                            log.error("❌ CircuitBreaker [{}]: ошибка - {}",
                                    name,
                                    event.getThrowable().getMessage()))
                    .onCallNotPermitted(event ->
                            log.warn("🚫 CircuitBreaker [{}]: вызов заблокирован (circuit OPEN)",
                                    name));
        });

        // Retry события
        retryRegistry.getAllRetries().forEach(retry -> {
            String name = retry.getName();

            retry.getEventPublisher()
                    .onRetry(event ->
                            log.warn("🔄 Retry [{}]: попытка #{}, причина: {}",
                                    name,
                                    event.getNumberOfRetryAttempts(),
                                    event.getLastThrowable().getMessage()))
                    .onSuccess(event ->
                            log.debug("✅ Retry [{}]: успех после {} попыток",
                                    name,
                                    event.getNumberOfRetryAttempts()))
                    .onError(event ->
                            log.error("❌ Retry [{}]: все {} попытки исчерпаны",
                                    name,
                                    event.getNumberOfRetryAttempts()));
        });

        log.info("📊 Resilience4j event listeners зарегистрированы");
    }
}

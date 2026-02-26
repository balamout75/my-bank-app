package com.mybank.cash.dto;

import java.util.Map;

/**
 * Событие уведомления, получаемое из Kafka.
 * Вместо REST NotificationRequest.
 * @param service     идентификатор сервиса-отправителя
 * @param operationId уникальный идентификатор операции (idempotency key)
 * @param username    логин пользователя
 * @param payload     произвольные данные операции
 */
public record NotificationEvent(
        String service,
        Long operationId,
        String username,
        Map<String, Object> payload
) {}

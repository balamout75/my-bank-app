package com.mybank.notifications.dto;

import java.util.Map;

public record NotificationEvent(
        String service,
        Long operationId,
        String username,
        Map<String, Object> payload
) {}

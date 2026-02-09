package com.mybank.cash.dto;

import java.util.Map;

public record NotificationRequest(
        Long operationId,
        String username,
        Map<String, Object> payload
) {}
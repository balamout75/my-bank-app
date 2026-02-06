package com.mybank.accounts.dto;

import java.util.Map;

public record NotificationRequest(
        Long operationId,
        String type,
        String username,
        String message,
        Map<String, Object> meta
) {}

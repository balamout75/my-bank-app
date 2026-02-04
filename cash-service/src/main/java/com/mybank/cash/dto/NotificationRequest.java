package com.mybank.cash.dto;

import java.util.Map;

public record NotificationRequest(
        String type,
        String username,
        String message,
        Map<String, Object> meta
) {}
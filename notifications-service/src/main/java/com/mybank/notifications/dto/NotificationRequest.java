package com.mybank.notifications.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

public record NotificationRequest(
        @NotNull Long operationId,
        @NotBlank String username,
        Map<String, Object> payload
) {}

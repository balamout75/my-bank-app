package com.mybank.frontend.client.dto;

public record UpdateResult(
        Long operationId,
        boolean notificationSent,
        String message)
{}
package com.mybank.accounts.dto;

public record UpdateResult(
        Long operationId,
        boolean notificationSent,
        String message)
{}
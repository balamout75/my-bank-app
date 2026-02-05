package com.mybank.notifications.model;

public enum OutboxStatus {
    NEW,
    IN_PROGRESS,
    RETRY,
    PUBLISHED
}

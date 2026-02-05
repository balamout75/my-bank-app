package com.mybank.notifications.service;

import com.mybank.notifications.model.OutboxEvent;

public interface OutboxEventPublisher {
    void publish(OutboxEvent event);
}

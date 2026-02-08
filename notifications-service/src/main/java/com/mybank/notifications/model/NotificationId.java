package com.mybank.notifications.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;

@Embeddable
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@EqualsAndHashCode
public class NotificationId implements Serializable {

    @Column(name = "service", nullable = false, length = 128)
    private String service;

    @Column(name = "operation_id", nullable = false)
    private Long operationId;
}

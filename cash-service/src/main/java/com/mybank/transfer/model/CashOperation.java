package com.mybank.transfer.model;

import com.mybank.transfer.dto.OperationStatus;
import com.mybank.transfer.dto.CashOperationType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "cash_operations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CashOperation {

    @Id
    @Column(name = "operation_id", nullable = false)
    private Long operationId;

    @Column(name = "username", nullable = false, length = 255)
    private String username;

    // RESERVED допускает NULL
    @Column(name = "amount", precision = 19, scale = 2, nullable = true)
    private BigDecimal amount;

    // RESERVED допускает NULL
    @Enumerated(EnumType.STRING)
    @Column(name = "type", length = 20, nullable = true)
    private CashOperationType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private OperationStatus status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "notification_attempts", nullable = false)
    private int notificationAttempts;

    @Column(name = "notification_error", columnDefinition = "TEXT")
    private String notificationError;

    @Column(name = "notification_attempts_at", nullable = false)
    private LocalDateTime notificationAttemptsAt;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        status = OperationStatus.RESERVED;
        notificationAttemptsAt = now;
        notificationAttempts = 0;
    }

    public void touch() {
        this.notificationAttemptsAt = LocalDateTime.now();
    }
}

package com.mybank.cash.model;

import com.mybank.cash.dto.OperationStatus;
import com.mybank.cash.dto.OperationType;
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
    @Column(name = "id", length = 36)
    private Long operationId;  // UUID — сам по себе PK и idempotency key

    @Column(nullable = false)
    private String username;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OperationType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OperationStatus status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "error_message")
    private String errorMessage;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (status == null) {
            status = OperationStatus.RESERVED;
        }
    }
}
package com.mybank.notifications.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(schema = "notifications", name = "outbox_event",
        uniqueConstraints = @UniqueConstraint(name = "uq_outbox_operation_event", columnNames = {"operation_id", "event_type"}))
@Getter
@Setter
public class OutboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "operation_id", nullable = false)
    private Long operationId;

    @Column(name = "aggregate_type", nullable = false, length = 64)
    private String aggregateType;

    @Column(name = "aggregate_id", nullable = false)
    private Long aggregateId;

    @Column(name = "event_type", nullable = false, length = 64)
    private String eventType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> payload;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private OutboxStatus status;

    @Column(name = "attempts", nullable = false)
    private int attempts;

    @Column(name = "next_attempt_at", nullable = false)
    private LocalDateTime nextAttemptAt;

    @Column(name = "locked_at")
    private LocalDateTime lockedAt;

    @Column(name = "locked_by", length = 64)
    private String lockedBy;

    @Column(name = "last_error", columnDefinition = "text")
    private String lastError;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}

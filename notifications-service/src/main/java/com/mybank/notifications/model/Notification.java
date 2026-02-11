package com.mybank.notifications.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(schema = "notifications", name = "notifications")
@Getter
@Setter
public class Notification {

    @EmbeddedId
    private NotificationId id;

    @Column(name = "username", nullable = false, length = 128)
    private String username;

    // JSON payload уведомления
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> payload;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private OperationStatus status;

    @Column(name = "attempts", nullable = false)
    private Integer attempts = 0;

    @Column(name = "error", columnDefinition = "text")
    private String error;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // --- lifecycle hooks ---
    @PrePersist
    public void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        this.attempts = 0;
        this.error = null;
    }

    public void touch() {
        this.updatedAt = LocalDateTime.now();
    }
}

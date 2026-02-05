package com.mybank.notifications.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(schema = "notifications", name = "notifications",
        uniqueConstraints = @UniqueConstraint(name = "uq_notifications_operation_id", columnNames = "operation_id"))
@Getter
@Setter
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "operation_id", nullable = false)
    private Long operationId;

    @Column(name = "type", nullable = false, length = 64)
    private String type;

    @Column(name = "username", nullable = false, length = 128)
    private String username;

    @Column(name = "message", columnDefinition = "text")
    private String message;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "meta", columnDefinition = "jsonb")
    private Map<String, Object> meta;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private NotificationStatus status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}

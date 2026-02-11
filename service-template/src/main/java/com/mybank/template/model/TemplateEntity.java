package com.mybank.template.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Пример Entity для шаблона
 * 
 * ====== ИЗМЕНИТЬ ПРИ КОПИРОВАНИИ ======
 * 1. Переименуйте класс (например: Account, CashOperation, Transfer)
 * 2. Измените название таблицы @Table(name = "...")
 * 3. Добавьте нужные поля для вашей сущности
 * 4. Удалите ненужные поля
 */
@Entity
@Table(name = "template_entities")  // ← ИЗМЕНИТЬ название таблицы!
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TemplateEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Пример поля
     */
    @Column(nullable = false)
    private String name;

    /**
     * Пример поля с уникальным значением
     */
    @Column(unique = true, nullable = false)
    private String code;

    /**
     * Пример текстового поля
     */
    @Column(columnDefinition = "TEXT")
    private String description;

    /**
     * Пример булева поля
     */
    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    /**
     * Дата создания (автоматически)
     */
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Дата обновления (автоматически)
     */
    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Версия для оптимистичной блокировки
     */
    @Version
    private Long version;

    // ====== ПРИМЕРЫ СВЯЗЕЙ (раскомментируйте при необходимости) ======

    /**
     * Пример Many-to-One связи
     */
    /*
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private ParentEntity parent;
    */

    /**
     * Пример One-to-Many связи
     */
    /*
    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ChildEntity> children = new ArrayList<>();
    */
}

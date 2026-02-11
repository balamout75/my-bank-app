package com.mybank.template.repository;

import com.mybank.template.model.TemplateEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository для TemplateEntity
 * 
 * ====== ИЗМЕНИТЬ ПРИ КОПИРОВАНИИ ======
 * 1. Переименуйте интерфейс (например: AccountRepository, CashOperationRepository)
 * 2. Измените тип Entity в JpaRepository<TemplateEntity, Long>
 * 3. Добавьте нужные методы запросов
 */
@Repository
public interface TemplateRepository extends JpaRepository<TemplateEntity, Long> {

    /**
     * Найти по коду
     */
    Optional<TemplateEntity> findByCode(String code);

    /**
     * Проверить существование по коду
     */
    boolean existsByCode(String code);

    /**
     * Найти все активные
     */
    List<TemplateEntity> findByActiveTrue();

    /**
     * Найти по имени (игнорируя регистр)
     */
    List<TemplateEntity> findByNameContainingIgnoreCase(String name);

    // ====== ПРИМЕРЫ CUSTOM QUERIES (раскомментируйте при необходимости) ======

    /**
     * Пример JPQL запроса
     */
    /*
    @Query("SELECT t FROM TemplateEntity t WHERE t.active = true AND t.createdAt >= :date")
    List<TemplateEntity> findActiveCreatedAfter(@Param("date") LocalDateTime date);
    */

    /**
     * Пример Native SQL запроса
     */
    /*
    @Query(value = "SELECT * FROM template_entities WHERE active = true", nativeQuery = true)
    List<TemplateEntity> findAllActiveNative();
    */

    /**
     * Пример запроса с пессимистичной блокировкой
     */
    /*
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM TemplateEntity t WHERE t.code = :code")
    Optional<TemplateEntity> findByCodeWithLock(@Param("code") String code);
    */
}

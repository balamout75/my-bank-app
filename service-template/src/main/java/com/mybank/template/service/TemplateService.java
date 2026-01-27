package com.mybank.template.service;

import com.mybank.template.dto.PageResponse;
import com.mybank.template.exception.ResourceNotFoundException;
import com.mybank.template.exception.ValidationException;
import com.mybank.template.dto.TemplateDTO;
import com.mybank.template.model.TemplateEntity;
import com.mybank.template.repository.TemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Сервис для работы с TemplateEntity
 * 
 * ====== ИЗМЕНИТЬ ПРИ КОПИРОВАНИИ ======
 * 1. Переименуйте класс (например: AccountService, CashService)
 * 2. Реализуйте нужную бизнес-логику
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class TemplateService {

    private final TemplateRepository repository;

    /**
     * Получить все записи с пагинацией
     */
    public PageResponse<TemplateDTO.Response> findAll(Pageable pageable) {
        log.debug("Finding all entities with pagination: {}", pageable);
        
        Page<TemplateEntity> page = repository.findAll(pageable);
        Page<TemplateDTO.Response> responsePage = page.map(this::toResponse);
        
        return PageResponse.fromPage(responsePage);
    }

    /**
     * Получить все активные записи
     */
    public List<TemplateDTO.Summary> findAllActive() {
        log.debug("Finding all active entities");
        
        return repository.findByActiveTrue().stream()
                .map(this::toSummary)
                .collect(Collectors.toList());
    }

    /**
     * Найти по ID
     */
    public TemplateDTO.Response findById(Long id) {
        log.debug("Finding entity by id: {}", id);
        
        TemplateEntity entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("TemplateEntity", "id", id));
        
        return toResponse(entity);
    }

    /**
     * Найти по коду
     */
    public TemplateDTO.Response findByCode(String code) {
        log.debug("Finding entity by code: {}", code);
        
        TemplateEntity entity = repository.findByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("TemplateEntity", "code", code));
        
        return toResponse(entity);
    }

    /**
     * Создать новую запись
     */
    @Transactional
    public TemplateDTO.Response create(TemplateDTO.CreateRequest request) {
        log.info("Creating new entity: {}", request);
        
        // Валидация: код должен быть уникальным
        if (repository.existsByCode(request.getCode())) {
            throw new ValidationException("Код уже существует: " + request.getCode());
        }
        
        TemplateEntity entity = TemplateEntity.builder()
                .name(request.getName())
                .code(request.getCode())
                .description(request.getDescription())
                .active(true)
                .build();
        
        TemplateEntity saved = repository.save(entity);
        log.info("Entity created with id: {}", saved.getId());
        
        return toResponse(saved);
    }

    /**
     * Обновить существующую запись
     */
    @Transactional
    public TemplateDTO.Response update(Long id, TemplateDTO.UpdateRequest request) {
        log.info("Updating entity {}: {}", id, request);
        
        TemplateEntity entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("TemplateEntity", "id", id));
        
        // Обновляем только непустые поля
        if (request.getName() != null) {
            entity.setName(request.getName());
        }
        if (request.getDescription() != null) {
            entity.setDescription(request.getDescription());
        }
        if (request.getActive() != null) {
            entity.setActive(request.getActive());
        }
        
        TemplateEntity updated = repository.save(entity);
        log.info("Entity {} updated successfully", id);
        
        return toResponse(updated);
    }

    /**
     * Удалить запись
     */
    @Transactional
    public void delete(Long id) {
        log.info("Deleting entity: {}", id);
        
        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException("TemplateEntity", "id", id);
        }
        
        repository.deleteById(id);
        log.info("Entity {} deleted successfully", id);
    }

    /**
     * Мягкое удаление (деактивация)
     */
    @Transactional
    public void deactivate(Long id) {
        log.info("Deactivating entity: {}", id);
        
        TemplateEntity entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("TemplateEntity", "id", id));
        
        entity.setActive(false);
        repository.save(entity);
        
        log.info("Entity {} deactivated successfully", id);
    }

    // ========== Маппинг Entity -> DTO ==========

    private TemplateDTO.Response toResponse(TemplateEntity entity) {
        return TemplateDTO.Response.builder()
                .id(entity.getId())
                .name(entity.getName())
                .code(entity.getCode())
                .description(entity.getDescription())
                .active(entity.getActive())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private TemplateDTO.Summary toSummary(TemplateEntity entity) {
        return TemplateDTO.Summary.builder()
                .id(entity.getId())
                .name(entity.getName())
                .code(entity.getCode())
                .active(entity.getActive())
                .build();
    }
}

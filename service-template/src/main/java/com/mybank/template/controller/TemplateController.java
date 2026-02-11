package com.mybank.template.controller;

import com.mybank.template.dto.ApiResponse;
import com.mybank.template.dto.PageResponse;
import com.mybank.template.dto.TemplateDTO;
import com.mybank.template.service.TemplateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller для Template API
 * 
 * ====== ИЗМЕНИТЬ ПРИ КОПИРОВАНИИ ======
 * 1. Переименуйте класс (например: AccountController, CashController)
 * 2. Измените @RequestMapping на нужный путь
 * 3. Реализуйте нужные endpoints
 */
@RestController
@RequestMapping("/api/template")  // ← ИЗМЕНИТЬ путь!
@RequiredArgsConstructor
@Slf4j
public class TemplateController {

    private final TemplateService service;

    /**
     * GET /api/template
     * Получить все записи с пагинацией
     */
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<TemplateDTO.Response>>> getAll(
            @PageableDefault(size = 20) Pageable pageable) {
        
        log.info("GET /api/template - page: {}, size: {}", 
                 pageable.getPageNumber(), pageable.getPageSize());
        
        PageResponse<TemplateDTO.Response> page = service.findAll(pageable);
        
        return ResponseEntity.ok(ApiResponse.success(page));
    }

    /**
     * GET /api/template/active
     * Получить все активные записи
     */
    @GetMapping("/active")
    public ResponseEntity<ApiResponse<List<TemplateDTO.Summary>>> getAllActive() {
        log.info("GET /api/template/active");
        
        List<TemplateDTO.Summary> list = service.findAllActive();
        
        return ResponseEntity.ok(ApiResponse.success(list));
    }

    /**
     * GET /api/template/{id}
     * Получить запись по ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TemplateDTO.Response>> getById(@PathVariable Long id) {
        log.info("GET /api/template/{}", id);
        
        TemplateDTO.Response response = service.findById(id);
        
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * GET /api/template/code/{code}
     * Получить запись по коду
     */
    @GetMapping("/code/{code}")
    public ResponseEntity<ApiResponse<TemplateDTO.Response>> getByCode(@PathVariable String code) {
        log.info("GET /api/template/code/{}", code);
        
        TemplateDTO.Response response = service.findByCode(code);
        
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * POST /api/template
     * Создать новую запись
     */
    @PostMapping
    public ResponseEntity<ApiResponse<TemplateDTO.Response>> create(
            @Valid @RequestBody TemplateDTO.CreateRequest request) {
        
        log.info("POST /api/template: {}", request);
        
        TemplateDTO.Response response = service.create(request);
        
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Запись создана успешно"));
    }

    /**
     * PUT /api/template/{id}
     * Обновить существующую запись
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<TemplateDTO.Response>> update(
            @PathVariable Long id,
            @Valid @RequestBody TemplateDTO.UpdateRequest request) {
        
        log.info("PUT /api/template/{}: {}", id, request);
        
        TemplateDTO.Response response = service.update(id, request);
        
        return ResponseEntity.ok(ApiResponse.success(response, "Запись обновлена успешно"));
    }

    /**
     * DELETE /api/template/{id}
     * Удалить запись
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        log.info("DELETE /api/template/{}", id);
        
        service.delete(id);
        
        return ResponseEntity.noContent().build();
    }

    /**
     * PATCH /api/template/{id}/deactivate
     * Деактивировать запись (мягкое удаление)
     */
    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<ApiResponse<String>> deactivate(@PathVariable Long id) {
        log.info("PATCH /api/template/{}/deactivate", id);
        
        service.deactivate(id);
        
        return ResponseEntity.ok(ApiResponse.success("Запись деактивирована"));
    }

    // ====== ПРИМЕРЫ ДОПОЛНИТЕЛЬНЫХ ENDPOINTS ======

    /**
     * POST /api/template/batch
     * Создать несколько записей за раз
     */
    /*
    @PostMapping("/batch")
    public ResponseEntity<ApiResponse<List<TemplateDTO.Response>>> createBatch(
            @Valid @RequestBody List<TemplateDTO.CreateRequest> requests) {
        
        log.info("POST /api/template/batch: {} items", requests.size());
        
        List<TemplateDTO.Response> responses = service.createBatch(requests);
        
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(responses, "Записи созданы успешно"));
    }
    */

    /**
     * GET /api/template/search
     * Поиск по параметрам
     */
    /*
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<TemplateDTO.Summary>>> search(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Boolean active) {
        
        log.info("GET /api/template/search?name={}&active={}", name, active);
        
        List<TemplateDTO.Summary> results = service.search(name, active);
        
        return ResponseEntity.ok(ApiResponse.success(results));
    }
    */
}

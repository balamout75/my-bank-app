package com.mybank.template.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO для Template сервиса
 * 
 * ====== ИЗМЕНИТЬ ПРИ КОПИРОВАНИИ ======
 * Переименуйте классы согласно вашему сервису
 */
public class TemplateDTO {

    /**
     * DTO для ответа (Response)
     * Возвращается из GET запросов
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Response {
        private Long id;
        private String name;
        private String code;
        private String description;
        private Boolean active;
        
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime createdAt;
        
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime updatedAt;
    }

    /**
     * DTO для создания (Create Request)
     * Используется в POST запросах
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CreateRequest {
        
        @NotBlank(message = "Имя не может быть пустым")
        @Size(min = 2, max = 100, message = "Имя должно быть от 2 до 100 символов")
        private String name;
        
        @NotBlank(message = "Код не может быть пустым")
        @Size(min = 2, max = 50, message = "Код должен быть от 2 до 50 символов")
        private String code;
        
        @Size(max = 500, message = "Описание не должно превышать 500 символов")
        private String description;
    }

    /**
     * DTO для обновления (Update Request)
     * Используется в PUT/PATCH запросах
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UpdateRequest {
        
        @Size(min = 2, max = 100, message = "Имя должно быть от 2 до 100 символов")
        private String name;
        
        @Size(max = 500, message = "Описание не должно превышать 500 символов")
        private String description;
        
        private Boolean active;
    }

    /**
     * DTO для краткой информации (Summary)
     * Используется в списках
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Summary {
        private Long id;
        private String name;
        private String code;
        private Boolean active;
    }
}

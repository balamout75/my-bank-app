package com.mybank.template.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Стандартный ответ для ошибок
 * Используется Global Exception Handler для всех ошибок
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {
    
    /**
     * HTTP статус код
     */
    private int status;
    
    /**
     * Тип ошибки
     */
    private String error;
    
    /**
     * Сообщение об ошибке
     */
    private String message;
    
    /**
     * Путь запроса, где произошла ошибка
     */
    private String path;
    
    /**
     * Timestamp ошибки
     */
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();
    
    /**
     * Детали ошибок валидации (если есть)
     */
    private List<ValidationError> validationErrors;
    
    /**
     * Дополнительные детали (для отладки, только в dev)
     */
    private String details;

    /**
     * Ошибка валидации поля
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ValidationError {
        /**
         * Имя поля
         */
        private String field;
        
        /**
         * Сообщение об ошибке
         */
        private String message;
        
        /**
         * Отклоненное значение (опционально)
         */
        private Object rejectedValue;
    }
}

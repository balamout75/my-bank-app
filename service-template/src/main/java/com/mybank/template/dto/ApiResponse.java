package com.mybank.template.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Стандартный ответ API
 * Используется для всех успешных ответов
 * 
 * @param <T> тип данных в ответе
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {
    
    /**
     * Данные ответа
     */
    private T data;
    
    /**
     * Сообщение (опционально)
     */
    private String message;
    
    /**
     * Timestamp ответа
     */
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();
    
    /**
     * Успешный ли ответ
     */
    @Builder.Default
    private boolean success = true;

    /**
     * Создать успешный ответ с данными
     */
    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .data(data)
                .success(true)
                .build();
    }

    /**
     * Создать успешный ответ с данными и сообщением
     */
    public static <T> ApiResponse<T> success(T data, String message) {
        return ApiResponse.<T>builder()
                .data(data)
                .message(message)
                .success(true)
                .build();
    }

    /**
     * Создать успешный ответ только с сообщением
     */
    public static <T> ApiResponse<T> success(String message) {
        return ApiResponse.<T>builder()
                .message(message)
                .success(true)
                .build();
    }
}

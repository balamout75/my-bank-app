package com.mybank.template.exception;

/**
 * Исключение валидации бизнес-правил
 * HTTP Status: 400 BAD REQUEST
 */
public class ValidationException extends BusinessException {
    
    public ValidationException(String message) {
        super(message, "VALIDATION_ERROR");
    }
    
    public ValidationException(String message, Throwable cause) {
        super(message, "VALIDATION_ERROR", cause);
    }
}

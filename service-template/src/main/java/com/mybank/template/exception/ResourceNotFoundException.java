package com.mybank.template.exception;

/**
 * Исключение, когда ресурс не найден
 * HTTP Status: 404 NOT FOUND
 */
public class ResourceNotFoundException extends BusinessException {
    
    public ResourceNotFoundException(String resourceName, String fieldName, Object fieldValue) {
        super(String.format("%s не найден с %s: '%s'", resourceName, fieldName, fieldValue), 
              "RESOURCE_NOT_FOUND");
    }
    
    public ResourceNotFoundException(String message) {
        super(message, "RESOURCE_NOT_FOUND");
    }
}

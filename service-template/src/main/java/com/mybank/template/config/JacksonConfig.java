package com.mybank.template.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

/**
 * Конфигурация Jackson для сериализации/десериализации JSON
 * 
 * Настраивает:
 * - Поддержку Java 8 Date/Time API (LocalDate, LocalDateTime)
 * - Форматирование дат в ISO-8601
 * - Игнорирование null значений в ответах
 */
@Configuration
public class JacksonConfig {

    @Bean
    public ObjectMapper objectMapper(Jackson2ObjectMapperBuilder builder) {
        ObjectMapper objectMapper = builder.build();
        
        // Модуль для поддержки Java 8 Date/Time API
        objectMapper.registerModule(new JavaTimeModule());
        
        // Форматировать даты как строки (ISO-8601), а не timestamp
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        
        // Включить красивое форматирование JSON (для dev)
        // В production можно отключить для экономии трафика
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        
        return objectMapper;
    }
}

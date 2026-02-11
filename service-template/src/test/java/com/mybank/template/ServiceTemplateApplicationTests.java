package com.mybank.template;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Базовый тест загрузки контекста Spring Boot
 */
@SpringBootTest
@ActiveProfiles("test")
class ServiceTemplateApplicationTests {

    @Test
    void contextLoads() {
        // Проверяем что контекст Spring успешно загружается
    }
}

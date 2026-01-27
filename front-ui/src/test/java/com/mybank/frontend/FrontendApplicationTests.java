package com.mybank.frontend;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

/**
 * Тест загрузки контекста Spring Boot
 */
@SpringBootTest
@TestPropertySource(properties = {
    // Отключаем OAuth2 для тестов
    "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientAutoConfiguration"
})
class FrontendApplicationTests {

    @Test
    void contextLoads() {
        // Проверяем что контекст Spring успешно загружается
    }
}

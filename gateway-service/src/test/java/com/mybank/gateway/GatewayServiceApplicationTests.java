package com.mybank.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Тест загрузки контекста Gateway Service
 */
@SpringBootTest
@ActiveProfiles("test")
class GatewayServiceApplicationTests {

    @Test
    void contextLoads() {
        // Проверяем что контекст Spring успешно загружается
        // и Gateway стартует без ошибок
    }
}

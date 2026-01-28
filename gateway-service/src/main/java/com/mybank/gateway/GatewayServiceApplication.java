package com.mybank.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * Gateway Service - API Gateway
 * 
 * Единая точка входа для всех клиентских запросов.
 * 
 * Функции:
 * - Маршрутизация запросов к микросервисам
 * - Load Balancing (балансировка нагрузки)
 * - Service Discovery интеграция (через Eureka)
 * - Фильтрация запросов/ответов
 * - CORS настройки
 * - Rate Limiting (ограничение запросов)
 * - Авторизация (OAuth2 интеграция)
 * 
 * Примеры маршрутизации:
 * - /api/accounts/** → accounts-service
 * - /api/cash/** → cash-service
 * - /api/transfer/** → transfer-service
 * 
 * Доступен на: http://localhost:8090
 */
@SpringBootApplication
@EnableDiscoveryClient
public class GatewayServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayServiceApplication.class, args);
    }
}

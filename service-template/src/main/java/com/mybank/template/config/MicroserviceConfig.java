package com.mybank.template.config;

import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Configuration;

/**
 * Базовая конфигурация для микросервисов
 * Включает Service Discovery (Eureka Client)
 * 
 * Микросервисы должны импортировать эту конфигурацию:
 * @Import(MicroserviceConfig.class)
 */
@Configuration
@EnableDiscoveryClient
public class MicroserviceConfig {
    // Базовая конфигурация для всех микросервисов
    // Автоматически регистрирует сервис в Eureka
}

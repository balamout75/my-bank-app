package com.mybank.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.config.server.EnableConfigServer;

/**
 * Config Service - Spring Cloud Config Server
 * 
 * Централизованный сервер конфигураций для всех микросервисов.
 * 
 * Функции:
 * - Хранение конфигураций всех сервисов в одном месте
 * - Управление конфигурациями для разных окружений (dev, prod, test)
 * - Динамическое обновление конфигураций без перезапуска сервисов
 * - Версионирование конфигураций (через Git)
 * 
 * Конфигурации хранятся:
 * - Native (файловая система) - для разработки
 * - Git (Git репозиторий) - для production
 * 
 * Доступ к конфигурациям:
 * http://localhost:8888/{application}/{profile}
 * http://localhost:8888/accounts-service/default
 * http://localhost:8888/accounts-service/prod
 */
@SpringBootApplication
@EnableConfigServer
public class ConfigServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ConfigServiceApplication.class, args);
    }
}

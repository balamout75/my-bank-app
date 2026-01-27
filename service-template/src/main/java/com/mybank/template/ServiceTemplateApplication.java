package com.mybank.template;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * ====== ИЗМЕНИТЬ ПРИ КОПИРОВАНИИ ======
 * 
 * 1. Переименуйте класс согласно вашему сервису:
 *    - AccountsServiceApplication
 *    - CashServiceApplication
 *    - TransferServiceApplication
 *    и т.д.
 * 
 * 2. Переименуйте пакет com.mybank.template в:
 *    - com.mybank.accounts
 *    - com.mybank.cash
 *    - com.mybank.transfer
 *    и т.д.
 * 
 * 3. Если нужен Feign Client, добавьте:
 *    @EnableFeignClients
 * 
 * 4. Обновите application.yml с правильным именем и портом
 */
@SpringBootApplication
@EnableDiscoveryClient  // Регистрация в Eureka (было в MicroserviceConfig)
public class ServiceTemplateApplication {

    public static void main(String[] args) {
        SpringApplication.run(ServiceTemplateApplication.class, args);
    }
}

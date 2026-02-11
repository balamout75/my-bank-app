package com.mybank.discovery;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

/**
 * Discovery Service - Eureka Server
 * 
 * Сервис регистрации и обнаружения микросервисов.
 * Все микросервисы регистрируются здесь и могут находить друг друга по имени.
 * 
 * Web Dashboard доступен по адресу: http://localhost:8761
 */
@SpringBootApplication
@EnableEurekaServer
public class DiscoveryServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(DiscoveryServiceApplication.class, args);
    }
}

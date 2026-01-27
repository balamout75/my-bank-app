# Service Template - Шаблон микросервиса

## 📋 Описание

Это **шаблон-заготовка** для быстрого создания новых микросервисов в банковском приложении.

**Полностью самодостаточный** - содержит весь необходимый код без внешних зависимостей!

**НЕ используйте этот модуль напрямую!** Скопируйте его и адаптируйте под свой сервис.

## 🚀 Как использовать шаблон

### Шаг 1: Скопировать директорию

```bash
# Из корня проекта my-bank-app
cp -r service-template accounts-service
cd accounts-service
```

### Шаг 2: Переименовать пакеты

**Глобальная замена в IDE:**

```
com.mybank.template → com.mybank.accounts   (или другое имя)
```

**Или вручную:**

```bash
# Переименовать директорию
mv src/main/java/com/mybank/template src/main/java/com/mybank/accounts
mv src/test/java/com/mybank/template src/test/java/com/mybank/accounts
```

### Шаг 3: Переименовать файлы

**Переименуйте следующие файлы:**

```
ServiceTemplateApplication.java   → AccountsServiceApplication.java
TemplateEntity.java               → Account.java
TemplateRepository.java           → AccountRepository.java
TemplateService.java              → AccountService.java
TemplateController.java           → AccountController.java
TemplateDTO.java                  → AccountDTO.java
```

### Шаг 4: Обновить pom.xml

```xml
<!-- ИЗМЕНИТЬ -->
<artifactId>accounts-service</artifactId>
<n>Accounts Service</n>
<description>Service for managing user accounts</description>
```

### Шаг 5: Обновить application.yml

```yaml
server:
  port: 8081  # ← Ваш порт

spring:
  application:
    name: accounts-service  # ← Имя вашего сервиса
  
  datasource:
    url: jdbc:postgresql://localhost:5432/mybank?currentSchema=accounts  # ← Схема БД
```

### Шаг 6: Обновить главный класс

```java
@SpringBootApplication
@Import(MicroserviceConfig.class)
// Если нужен Feign Client:
// @EnableFeignClients
public class AccountsServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(AccountsServiceApplication.class, args);
    }
}
```

### Шаг 7: Адаптировать Entity

Измените `Account.java` (бывший `TemplateEntity.java`):

```java
@Entity
@Table(name = "accounts")  // ← Название таблицы
public class Account {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    // Добавьте нужные поля для вашей сущности
    private String username;
    private String firstName;
    private String lastName;
    private BigDecimal balance;
    // ...
}
```

### Шаг 8: Обновить Repository

```java
@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {
    Optional<Account> findByUsername(String username);
    // Добавьте нужные методы
}
```

### Шаг 9: Реализовать Service

Добавьте бизнес-логику в `AccountService.java`

### Шаг 10: Настроить Controller

```java
@RestController
@RequestMapping("/api/accounts")  // ← Путь API
public class AccountController {
    // Реализуйте нужные endpoints
}
```

### Шаг 11: Добавить в корневой pom.xml

```xml
<modules>
    <module>common-lib</module>
    <module>front-ui</module>
    <module>discovery-service</module>
    <module>accounts-service</module>  ← Добавить
</modules>
```

### Шаг 12: Собрать и запустить

```bash
# Из корня проекта
mvn clean install

# Запустить сервис
mvn spring-boot:run -pl accounts-service
```

## 📦 Что включено в шаблон

### Структура

```
service-template/
├── pom.xml                                    # Maven конфигурация
├── src/
│   ├── main/
│   │   ├── java/com/mybank/template/
│   │   │   ├── ServiceTemplateApplication.java    # Главный класс
│   │   │   ├── model/
│   │   │   │   └── TemplateEntity.java           # Пример Entity
│   │   │   ├── repository/
│   │   │   │   └── TemplateRepository.java       # Пример Repository
│   │   │   ├── service/
│   │   │   │   └── TemplateService.java          # Пример Service
│   │   │   ├── controller/
│   │   │   │   └── TemplateController.java       # Пример Controller
│   │   │   └── dto/
│   │   │       └── TemplateDTO.java              # Примеры DTO
│   │   └── resources/
│   │       └── application.yml                    # Конфигурация
│   └── test/
│       ├── java/com/mybank/template/
│       │   └── ServiceTemplateApplicationTests.java
│       └── resources/
│           └── application-test.yml               # Тест конфигурация
└── README.md
```

### Зависимости (pom.xml)

✅ **Уже включены:**
- Common Library (наше шасси)
- Spring Boot Web
- Spring Data JPA
- PostgreSQL Driver
- Validation
- Actuator
- Eureka Client
- Lombok
- DevTools

⚠️ **Закомментированы** (раскомментируйте при необходимости):
- Spring Security + OAuth2 Resource Server
- Feign Client
- Circuit Breaker (Resilience4j)
- Testcontainers

### Конфигурация (application.yml)

Настроены:
- ✅ Server port (нужно изменить)
- ✅ Application name (нужно изменить)
- ✅ PostgreSQL datasource
- ✅ JPA/Hibernate
- ✅ Eureka Client
- ✅ Actuator endpoints
- ✅ Logging

### Примеры кода

**Entity** (TemplateEntity.java):
- JPA аннотации
- Timestamps (@CreationTimestamp, @UpdateTimestamp)
- Optimistic locking (@Version)
- Примеры связей (закомментированы)

**Repository** (TemplateRepository.java):
- Extends JpaRepository
- Примеры Query методов
- Примеры @Query (JPQL и Native SQL)
- Примеры с блокировками

**Service** (TemplateService.java):
- CRUD операции
- Пагинация
- Транзакции (@Transactional)
- Маппинг Entity ↔ DTO
- Использование common-lib exceptions

**Controller** (TemplateController.java):
- REST endpoints (GET, POST, PUT, DELETE, PATCH)
- Валидация (@Valid)
- Использование ApiResponse из common-lib
- Пагинация
- Примеры дополнительных endpoints

**DTO** (TemplateDTO.java):
- Response DTO
- CreateRequest DTO
- UpdateRequest DTO
- Summary DTO
- Валидация (@NotBlank, @Size)

## 🎯 Best Practices

### 1. Именование

```
Сервис:          accounts-service, cash-service
Главный класс:   AccountsServiceApplication
Пакет:           com.mybank.accounts
Entity:          Account
Repository:      AccountRepository
Service:         AccountService
Controller:      AccountController
```

### 2. Порты

Придерживайтесь соглашения:
- 8761 - Discovery Service
- 8888 - Config Service
- 9000 - OAuth2 Server
- 8090 - Gateway Service
- 8080 - Front UI
- 8081 - Accounts Service
- 8082 - Cash Service
- 8083 - Transfer Service
- 8084 - Notifications Service

### 3. API пути

```
/api/{service-name}
/api/accounts
/api/cash
/api/transfer
```

### 4. Схемы БД

Каждый сервис использует свою схему PostgreSQL:
```sql
currentSchema=accounts
currentSchema=cash
currentSchema=transfer
currentSchema=notifications
```

### 5. Логирование

```java
@Slf4j
public class MyService {
    public void doSomething() {
        log.info("Important business action");
        log.debug("Detailed information for debugging");
        log.error("Error occurred", exception);
    }
}
```

### 6. Обработка ошибок

Используйте исключения из common-lib:

```java
// 404 Not Found
throw new ResourceNotFoundException("Account", "id", id);

// 400 Bad Request (валидация)
throw new ValidationException("Username уже существует");

// 400 Bad Request (баланс)
throw new InsufficientBalanceException(required, available);
```

### 7. Ответы API

Используйте ApiResponse из common-lib:

```java
// Успешный ответ
return ResponseEntity.ok(ApiResponse.success(data));

// С сообщением
return ResponseEntity.ok(ApiResponse.success(data, "Операция успешна"));

// Создание (201)
return ResponseEntity
    .status(HttpStatus.CREATED)
    .body(ApiResponse.success(data, "Создано"));
```

## 🔧 Дополнительная настройка

### Добавить OAuth2 Security

1. Раскомментируйте в pom.xml:
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-oauth2-resource-server</artifactId>
</dependency>
```

2. Создайте SecurityConfig:
```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) {
        return http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/**").permitAll()
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2.jwt())
            .build();
    }
}
```

3. Раскомментируйте в application.yml:
```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: http://localhost:9000
```

### Добавить Feign Client

1. Раскомментируйте в pom.xml:
```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-openfeign</artifactId>
</dependency>
```

2. Добавьте аннотацию:
```java
@SpringBootApplication
@EnableFeignClients
public class MyServiceApplication { ... }
```

3. Создайте клиент:
```java
@FeignClient(name = "other-service")
public interface OtherServiceClient {
    @GetMapping("/api/data")
    Data getData();
}
```

## 📝 Чек-лист после копирования

- [ ] Переименовал директорию `service-template` → `my-service`
- [ ] Переименовал пакет `com.mybank.template` → `com.mybank.myservice`
- [ ] Переименовал все классы (Application, Entity, Repository, Service, Controller, DTO)
- [ ] Обновил `artifactId` в pom.xml
- [ ] Изменил `server.port` в application.yml
- [ ] Изменил `spring.application.name` в application.yml
- [ ] Изменил схему БД в `datasource.url`
- [ ] Изменил название таблицы в `@Table(name = "...")`
- [ ] Изменил путь API в `@RequestMapping`
- [ ] Реализовал нужные поля Entity
- [ ] Реализовал нужные методы Repository
- [ ] Реализовал бизнес-логику Service
- [ ] Реализовал endpoints Controller
- [ ] Обновил DTO классы
- [ ] Добавил модуль в корневой pom.xml
- [ ] Протестировал компиляцию: `mvn clean install`
- [ ] Запустил сервис: `mvn spring-boot:run`
- [ ] Проверил регистрацию в Eureka: http://localhost:8761

## ✅ Готовые примеры

После создания шаблона, в проекте будут созданы полные рабочие сервисы:
- accounts-service
- cash-service
- transfer-service
- notifications-service

Можете использовать их как примеры!

---

**Шаблон готов к копированию и использованию!** 🎉

**Следуйте инструкциям выше для создания нового микросервиса.**

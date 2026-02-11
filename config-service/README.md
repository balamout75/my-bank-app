# Config Service - Централизованная конфигурация

## Описание

**Config Service** - это централизованный сервер конфигураций на базе **Spring Cloud Config Server**.

### Зачем нужен?

В микросервисной архитектуре каждый сервис имеет свою конфигурацию. Config Service решает проблемы:

1. 📁 **Централизация** - все конфигурации в одном месте
2. 🔄 **Версионирование** - история изменений через Git
3. 🌍 **Окружения** - разные конфигурации для dev/test/prod
4. 🔥 **Динамическое обновление** - изменение конфигураций без перезапуска
5. 🔐 **Безопасность** - credentials не в коде, а в Config Server
6. 👥 **Централизованное управление** - один источник правды

## Структура модуля

```
config-service/
├── pom.xml
├── README.md
└── src/
    ├── main/
    │   ├── java/com/mybank/config/
    │   │   └── ConfigServiceApplication.java    # @EnableConfigServer
    │   └── resources/
    │       ├── application.yml                   # Настройки Config Server
    │       └── config/                           # Конфигурации сервисов
    │           ├── application.yml               # Общая конфигурация
    │           ├── accounts-service.yml          # Accounts Service
    │           ├── accounts-service-prod.yml     # Accounts Service (prod)
    │           ├── cash-service.yml              # Cash Service
    │           └── transfer-service.yml          # Transfer Service
    └── test/
        └── java/com/mybank/config/
            └── ConfigServiceApplicationTests.java
```

## Технологии

- **Spring Boot 4.0.2**
- **Spring Cloud Config Server 2025.1.0**
- **Spring Cloud Netflix Eureka Client**

## Конфигурация

### Port
- **8888** - стандартный порт Config Server

### Режимы работы

#### 1. Native (файловая система)

**Для разработки** - конфигурации хранятся в `classpath:/config`

```yaml
spring:
  cloud:
    config:
      server:
        native:
          search-locations: classpath:/config
  profiles:
    active: native
```

#### 2. Git (Git репозиторий)

**Для production** - конфигурации в Git репозитории

```yaml
spring:
  cloud:
    config:
      server:
        git:
          uri: https://github.com/your-org/config-repo
          default-label: main
          search-paths: '{application}'
          username: ${GIT_USERNAME}
          password: ${GIT_PASSWORD}
```

## Как работает

### 1. Config Server стартует

```bash
mvn spring-boot:run
```

Config Server доступен на `http://localhost:8888`

### 2. Клиенты запрашивают конфигурацию

Микросервисы при старте обращаются к Config Server:

```
accounts-service → GET http://localhost:8888/accounts-service/default
                ← получает accounts-service.yml
```

### 3. Config Server отдает конфигурацию

```yaml
# Ответ Config Server
server:
  port: 8081
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/mybank?currentSchema=accounts
...
```

### 4. Микросервис применяет конфигурацию

Сервис стартует с настройками из Config Server!

## Endpoints

### Config Server API

**Получить конфигурацию:**

```bash
# Формат: /{application}/{profile}
GET http://localhost:8888/accounts-service/default
GET http://localhost:8888/accounts-service/prod
GET http://localhost:8888/cash-service/default

# Формат: /{application}/{profile}/{label}
GET http://localhost:8888/accounts-service/prod/main

# Формат: /{application}-{profile}.yml
GET http://localhost:8888/accounts-service-prod.yml

# Формат: /{label}/{application}-{profile}.yml
GET http://localhost:8888/main/accounts-service-prod.yml
```

**Примеры запросов:**

```bash
# Default profile
curl http://localhost:8888/accounts-service/default

# Production profile
curl http://localhost:8888/accounts-service/prod

# В формате YAML
curl http://localhost:8888/accounts-service-default.yml

# В формате JSON
curl http://localhost:8888/accounts-service-default.json

# В формате Properties
curl http://localhost:8888/accounts-service-default.properties
```

### Actuator Endpoints

```bash
# Health check
GET http://localhost:8888/actuator/health

# Информация
GET http://localhost:8888/actuator/info

# Обновить конфигурацию
POST http://localhost:8888/actuator/refresh

# Environment
GET http://localhost:8888/actuator/env
```

## Структура конфигураций

### Иерархия применения:

```
1. application.yml            # Общие настройки для всех
2. {service}.yml              # Специфичные для сервиса
3. {service}-{profile}.yml    # Специфичные для профиля
```

**Пример для accounts-service в prod:**

```
application.yml             # Базовые настройки
    ↓
accounts-service.yml        # Настройки accounts-service
    ↓
accounts-service-prod.yml   # Production настройки
    ↓
Финальная конфигурация
```

### Примеры файлов конфигураций

#### application.yml (общие настройки)

```yaml
# Применяется ко всем сервисам
spring:
  jpa:
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect

eureka:
  instance:
    prefer-ip-address: true

logging:
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} - %msg%n"
```

#### accounts-service.yml

```yaml
# Специфичная конфигурация для accounts-service
server:
  port: 8081

spring:
  application:
    name: accounts-service
  datasource:
    url: jdbc:postgresql://localhost:5432/mybank?currentSchema=accounts
```

#### accounts-service-prod.yml

```yaml
# Production конфигурация для accounts-service
spring:
  datasource:
    url: jdbc:postgresql://prod-db:5432/mybank_prod?currentSchema=accounts
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}

logging:
  level:
    root: WARN
```

## Настройка клиентов (микросервисов)

### Шаг 1: Добавить зависимость

В `pom.xml` микросервиса:

```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-config</artifactId>
</dependency>
```

### Шаг 2: Создать bootstrap.yml

В микросервисе создать `src/main/resources/bootstrap.yml`:

```yaml
spring:
  application:
    name: accounts-service  # Имя сервиса
  
  cloud:
    config:
      uri: http://localhost:8888  # URL Config Server
      fail-fast: true              # Падать если Config Server недоступен
      retry:
        max-attempts: 6            # Количество попыток
  
  profiles:
    active: default  # Профиль (default, dev, prod)
```

### Шаг 3: Запустить микросервис

```bash
mvn spring-boot:run
```

Микросервис автоматически:
1. Подключается к Config Server
2. Запрашивает свою конфигурацию
3. Применяет полученные настройки
4. Стартует

## Динамическое обновление конфигураций

### Вариант 1: @RefreshScope

```java
@RestController
@RefreshScope  // Обновляется при вызове /actuator/refresh
public class MyController {
    
    @Value("${my.property}")
    private String myProperty;
}
```

Обновить конфигурацию:

```bash
# 1. Изменить конфигурацию в Config Server
# 2. Вызвать refresh в клиенте
curl -X POST http://localhost:8081/actuator/refresh
```

### Вариант 2: Spring Cloud Bus

С использованием RabbitMQ или Kafka можно обновлять конфигурации всех сервисов одним запросом:

```bash
# Обновить все сервисы сразу
curl -X POST http://localhost:8888/actuator/bus-refresh
```

## Безопасность

### Шифрование credentials

Config Server поддерживает шифрование:

```yaml
# Зашифрованное значение
spring:
  datasource:
    password: '{cipher}AQBZfUKOV8XTrZmZ...'
```

Шифрование/дешифрование:

```bash
# Зашифровать
curl http://localhost:8888/encrypt -d "myPassword"

# Расшифровать
curl http://localhost:8888/decrypt -d "{cipher}AQBZfUKOV8XTrZmZ..."
```

### Basic Auth

Защитить Config Server:

```yaml
# Config Server
spring:
  security:
    user:
      name: config
      password: secret
```

```yaml
# Клиент
spring:
  cloud:
    config:
      uri: http://localhost:8888
      username: config
      password: secret
```

## Production рекомендации

### 1. Использовать Git

```yaml
spring:
  cloud:
    config:
      server:
        git:
          uri: https://github.com/your-org/config-repo
          default-label: main
```

**Преимущества:**
- История изменений
- Pull requests для review
- Rollback при проблемах

### 2. Несколько инстансов Config Server

```
Config Server 1 (8888)
Config Server 2 (8889)
Config Server 3 (8890)
```

Клиенты используют список:

```yaml
spring:
  cloud:
    config:
      uri: http://config1:8888,http://config2:8889,http://config3:8890
```

### 3. Service Discovery

Регистрировать Config Server в Eureka:

```yaml
eureka:
  client:
    register-with-eureka: true
```

Клиенты находят через Eureka:

```yaml
spring:
  cloud:
    config:
      discovery:
        enabled: true
        service-id: config-service
```

### 4. Мониторинг

- Actuator health checks
- Prometheus metrics
- Логирование доступа

### 5. Безопасность

- Шифрование credentials
- HTTPS
- Basic Auth или OAuth2

## Git Repository структура

Для production создайте Git репозиторий:

```
config-repo/
├── application.yml                   # Общие настройки
├── application-prod.yml              # Production общие
├── accounts-service.yml              # Accounts Service (dev)
├── accounts-service-prod.yml         # Accounts Service (prod)
├── cash-service.yml                  # Cash Service (dev)
├── cash-service-prod.yml             # Cash Service (prod)
├── transfer-service.yml              # Transfer Service (dev)
├── transfer-service-prod.yml         # Transfer Service (prod)
└── README.md
```

## Запуск

### Локально

```bash
cd config-service
mvn spring-boot:run
```

### Из корня проекта

```bash
mvn spring-boot:run -pl config-service
```

### Docker (когда будет Dockerfile)

```bash
docker build -t config-service .
docker run -p 8888:8888 config-service
```

## Health Check

```bash
curl http://localhost:8888/actuator/health
```

Ожидаемый ответ:

```json
{
  "status": "UP"
}
```

## Проверка конфигураций

```bash
# Проверить что конфигурации доступны
curl http://localhost:8888/accounts-service/default

# Проверить production конфигурацию
curl http://localhost:8888/accounts-service/prod
```

## Troubleshooting

### Config Server не стартует

1. Проверить что порт 8888 свободен
2. Проверить что Eureka доступна (если используется)
3. Проверить логи

### Клиент не может получить конфигурацию

1. Проверить что Config Server запущен
2. Проверить URL в bootstrap.yml
3. Проверить имя приложения (spring.application.name)
4. Проверить что файл конфигурации существует

### Конфигурация не обновляется

1. Проверить что используется @RefreshScope
2. Вызвать /actuator/refresh
3. Проверить что изменения сохранены в Config Server

## Интеграция с другими сервисами

Config Service используется всеми микросервисами:

1. ✅ **Accounts Service** - получает настройки БД, порт
2. ✅ **Cash Service** - получает настройки БД, порт
3. ✅ **Transfer Service** - получает настройки БД, порт
4. ✅ **Gateway Service** - получает routing конфигурацию
5. ✅ **OAuth2 Server** - получает настройки безопасности

---

**Config Service готов!** 🎉

**Порт:** 8888
**API:** http://localhost:8888/{application}/{profile}
**Status:** ✅ ГОТОВ К ЗАПУСКУ

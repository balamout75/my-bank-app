# Gateway Service - API Gateway

## Описание

**Gateway Service** - это единая точка входа (API Gateway) для всех клиентских запросов на базе **Spring Cloud Gateway**.

### Зачем нужен?

В микросервисной архитектуре клиенты не должны напрямую обращаться к микросервисам. Gateway решает эту проблему:

1. 🚪 **Единая точка входа** - один URL для всех сервисов
2. 🔀 **Маршрутизация** - перенаправление запросов к нужным сервисам
3. ⚖️ **Load Balancing** - распределение нагрузки между инстансами
4. 🔍 **Service Discovery** - автоматическое обнаружение сервисов через Eureka
5. 🔐 **Авторизация** - централизованная проверка прав доступа
6. 🛡️ **Rate Limiting** - ограничение количества запросов
7. 📊 **Мониторинг** - логирование всех запросов
8. 🌐 **CORS** - настройка cross-origin requests
9. 🔄 **Circuit Breaker** - защита от каскадных отказов

## Структура модуля

```
gateway-service/
├── pom.xml
├── README.md
└── src/
    ├── main/
    │   ├── java/com/mybank/gateway/
    │   │   ├── GatewayServiceApplication.java   # Главный класс
    │   │   ├── config/
    │   │   │   └── CorsConfig.java              # CORS конфигурация
    │   │   └── filter/
    │   │       ├── LoggingFilter.java           # Логирование запросов
    │   │       └── RequestIdFilter.java         # X-Request-ID header
    │   └── resources/
    │       └── application.yml                   # Routes конфигурация
    └── test/
        ├── java/com/mybank/gateway/
        │   └── GatewayServiceApplicationTests.java
        └── resources/
            └── application-test.yml
```

## Технологии

- **Spring Boot 4.0.2**
- **Spring Cloud Gateway 2025.1.0**
- **Spring Cloud Netflix Eureka Client**
- **Spring Cloud LoadBalancer**

## Конфигурация

### Port
- **8090** - Gateway порт

### Routes (Маршруты)

Gateway перенаправляет запросы на основе пути:

```
Client Request              Gateway                  Microservice
─────────────────────────────────────────────────────────────────
GET /api/accounts/1    →    Gateway :8090    →    accounts-service:8081
GET /api/cash/deposit  →    Gateway :8090    →    cash-service:8082
POST /api/transfer     →    Gateway :8090    →    transfer-service:8083
```

### Настройка Routes (application.yml)

```yaml
spring:
  cloud:
    gateway:
      routes:
        # Accounts Service
        - id: accounts-service
          uri: lb://accounts-service  # lb = Load Balancer
          predicates:
            - Path=/api/accounts/**
          filters:
            - StripPrefix=1  # /api/accounts/1 → /accounts/1
        
        # Cash Service
        - id: cash-service
          uri: lb://cash-service
          predicates:
            - Path=/api/cash/**
          filters:
            - StripPrefix=1
        
        # Transfer Service
        - id: transfer-service
          uri: lb://transfer-service
          predicates:
            - Path=/api/transfer/**
          filters:
            - StripPrefix=1
```

### Load Balancer

`lb://service-name` означает:
1. Найти сервис `service-name` в Eureka
2. Получить список доступных инстансов
3. Выбрать один (round-robin по умолчанию)
4. Отправить запрос

## Как работает

### Архитектура:

```
┌─────────────┐
│   Client    │
│  (Browser)  │
└──────┬──────┘
       │ GET /api/accounts/123
       ↓
┌─────────────────────────────────┐
│    Gateway Service (:8090)      │
│                                 │
│  1. RequestIdFilter             │ ← Добавить X-Request-ID
│  2. LoggingFilter               │ ← Залогировать запрос
│  3. Route Matching              │ ← Найти route
│  4. Load Balancer               │ ← Выбрать инстанс
│  5. Forward Request             │ ← Отправить запрос
└────────┬────────────────────────┘
         │
         ↓
    ┌────────────┐
    │  Eureka    │ ← Найти accounts-service
    │  :8761     │
    └────────────┘
         │
         ↓
┌─────────────────┐
│ Accounts Service│
│    :8081        │ ← GET /accounts/123
└─────────────────┘
```

### Процесс обработки запроса:

```
1. Client отправляет: GET http://localhost:8090/api/accounts/123
   ↓
2. Gateway получает запрос
   ↓
3. RequestIdFilter добавляет X-Request-ID: uuid
   ↓
4. LoggingFilter логирует: "Incoming request: GET /api/accounts/123"
   ↓
5. Gateway ищет route: Path=/api/accounts/** → accounts-service
   ↓
6. StripPrefix=1: /api/accounts/123 → /accounts/123
   ↓
7. Load Balancer обращается к Eureka: "Где accounts-service?"
   ↓
8. Eureka отвечает: "accounts-service на localhost:8081"
   ↓
9. Gateway отправляет: GET http://localhost:8081/accounts/123
   ↓
10. Accounts Service обрабатывает и отвечает
   ↓
11. Gateway отправляет ответ клиенту
   ↓
12. LoggingFilter логирует: "Completed request: 200 OK - 145ms"
```

## Фильтры

### 1. RequestIdFilter

Добавляет уникальный ID к каждому запросу:

```
Request:
X-Request-ID: 550e8400-e29b-41d4-a716-446655440000

Response:
X-Request-ID: 550e8400-e29b-41d4-a716-446655440000
```

**Применение:**
- Трассировка запросов через микросервисы
- Корреляция логов
- Отладка в production

### 2. LoggingFilter

Логирует все запросы:

```
2024-01-27 11:00:00 - Incoming request: GET /api/accounts/123 - User-Agent: Mozilla/5.0
2024-01-27 11:00:00 - Completed request: GET /api/accounts/123 - Status: 200 - Duration: 145ms
```

### 3. Built-in Filters

**StripPrefix** - убирает префикс из пути:
```yaml
filters:
  - StripPrefix=1  # /api/accounts/123 → /accounts/123
  - StripPrefix=2  # /api/v1/accounts/123 → /accounts/123
```

**AddRequestHeader** - добавляет header:
```yaml
filters:
  - AddRequestHeader=X-Custom-Header, CustomValue
```

**AddResponseHeader** - добавляет header в ответ:
```yaml
filters:
  - AddResponseHeader=X-Response-Time, ${responseTime}
```

**Retry** - повторить запрос при ошибке:
```yaml
filters:
  - name: Retry
    args:
      retries: 3
      statuses: BAD_GATEWAY,GATEWAY_TIMEOUT
```

**CircuitBreaker** - защита от каскадных отказов:
```yaml
filters:
  - name: CircuitBreaker
    args:
      name: myCircuitBreaker
      fallbackUri: forward:/fallback
```

## Predicates (Условия маршрутизации)

### Path

Маршрутизация по пути:
```yaml
predicates:
  - Path=/api/accounts/**  # Любой путь начинающийся с /api/accounts/
```

### Method

По HTTP методу:
```yaml
predicates:
  - Method=GET,POST
```

### Header

По наличию header:
```yaml
predicates:
  - Header=X-Request-Id, \d+
```

### Query

По query параметру:
```yaml
predicates:
  - Query=version, 2  # ?version=2
```

### Host

По хосту:
```yaml
predicates:
  - Host=**.mybank.com
```

### Комбинирование

```yaml
predicates:
  - Path=/api/accounts/**
  - Method=GET
  - Header=X-API-Key
```

## CORS Configuration

### Глобальная конфигурация

```yaml
spring:
  cloud:
    gateway:
      globalcors:
        cors-configurations:
          '[/**]':
            allowed-origins: "*"
            allowed-methods:
              - GET
              - POST
              - PUT
              - DELETE
            allowed-headers: "*"
            allow-credentials: true
```

### Java конфигурация

```java
@Configuration
public class CorsConfig {
    @Bean
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration corsConfig = new CorsConfiguration();
        corsConfig.setAllowedOriginPatterns(List.of("*"));
        // ...
        return new CorsWebFilter(source);
    }
}
```

**⚠️ Production:** Замените `*` на конкретные домены!

```yaml
allowed-origins:
  - https://mybank.com
  - https://www.mybank.com
  - https://mobile.mybank.com
```

## Service Discovery Integration

Gateway автоматически находит сервисы через Eureka:

```yaml
spring:
  cloud:
    gateway:
      discovery:
        locator:
          enabled: true  # Автоматические routes
          lower-case-service-id: true
```

С этой настройкой Gateway автоматически создаст routes:

```
/accounts-service/** → lb://accounts-service
/cash-service/** → lb://cash-service
/transfer-service/** → lb://transfer-service
```

**Ручные routes имеют приоритет!**

## Load Balancing

Gateway использует Spring Cloud LoadBalancer:

### Round Robin (по умолчанию)

```
Request 1 → accounts-service:8081 (instance 1)
Request 2 → accounts-service:8082 (instance 2)
Request 3 → accounts-service:8083 (instance 3)
Request 4 → accounts-service:8081 (instance 1) ← снова
```

### Random

```java
@Bean
public ReactorLoadBalancer<ServiceInstance> randomLoadBalancer(
        Environment environment, 
        LoadBalancerClientFactory loadBalancerClientFactory) {
    String name = environment.getProperty(LoadBalancerClientFactory.PROPERTY_NAME);
    return new RandomLoadBalancer(
        loadBalancerClientFactory.getLazyProvider(name, ServiceInstanceListSupplier.class),
        name
    );
}
```

## Rate Limiting

Ограничение количества запросов (требует Redis):

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: accounts-service
          filters:
            - name: RequestRateLimiter
              args:
                redis-rate-limiter.replenishRate: 10  # 10 запросов в секунду
                redis-rate-limiter.burstCapacity: 20  # Burst до 20
```

```xml
<!-- Добавить в pom.xml -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis-reactive</artifactId>
</dependency>
```

## Circuit Breaker (Resilience4j)

Защита от каскадных отказов:

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: accounts-service
          filters:
            - name: CircuitBreaker
              args:
                name: accountsCircuitBreaker
                fallbackUri: forward:/fallback/accounts

resilience4j:
  circuitbreaker:
    configs:
      default:
        sliding-window-size: 10
        failure-rate-threshold: 50
        wait-duration-in-open-state: 10000
```

Fallback controller:

```java
@RestController
public class FallbackController {
    
    @GetMapping("/fallback/accounts")
    public Mono<Map<String, String>> accountsFallback() {
        return Mono.just(Map.of(
            "error", "Accounts service unavailable",
            "message", "Please try again later"
        ));
    }
}
```

## OAuth2 Integration

Защита Gateway с OAuth2:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-oauth2-resource-server</artifactId>
</dependency>
```

```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: http://localhost:9000
```

```java
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {
    
    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
            .authorizeExchange(exchanges -> exchanges
                .pathMatchers("/actuator/**").permitAll()
                .anyExchange().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2.jwt())
            .build();
    }
}
```

## Endpoints

### Gateway Routes

```bash
# Просмотр всех routes
GET http://localhost:8090/actuator/gateway/routes

# Детали конкретного route
GET http://localhost:8090/actuator/gateway/routes/accounts-service

# Refresh routes (после изменения конфигурации)
POST http://localhost:8090/actuator/gateway/refresh
```

### Actuator

```bash
# Health check
GET http://localhost:8090/actuator/health

# Metrics
GET http://localhost:8090/actuator/metrics

# Gateway metrics
GET http://localhost:8090/actuator/metrics/spring.cloud.gateway.requests
```

## Запуск

### Локально

```bash
cd gateway-service
mvn spring-boot:run
```

### Из корня проекта

```bash
mvn spring-boot:run -pl gateway-service
```

### Проверка

```bash
# Health check
curl http://localhost:8090/actuator/health

# Просмотр routes
curl http://localhost:8090/actuator/gateway/routes

# Проксирование через Gateway
curl http://localhost:8090/api/accounts/1
```

## Примеры запросов

### Через Gateway

```bash
# Accounts Service
curl http://localhost:8090/api/accounts
curl http://localhost:8090/api/accounts/123

# Cash Service
curl -X POST http://localhost:8090/api/cash/deposit \
  -H "Content-Type: application/json" \
  -d '{"amount": 100}'

# Transfer Service
curl -X POST http://localhost:8090/api/transfer \
  -H "Content-Type: application/json" \
  -d '{"from": "user1", "to": "user2", "amount": 50}'
```

### Request ID трассировка

```bash
# Запрос с custom Request ID
curl http://localhost:8090/api/accounts/123 \
  -H "X-Request-ID: my-custom-id"

# Response будет содержать тот же ID
# X-Request-ID: my-custom-id
```

## Troubleshooting

### Gateway не может найти сервис

1. Проверить что сервис зарегистрирован в Eureka:
   ```bash
   curl http://localhost:8761
   ```

2. Проверить имя сервиса в `spring.application.name`

3. Проверить что Gateway подключен к Eureka:
   ```yaml
   eureka:
     client:
       register-with-eureka: true
       fetch-registry: true
   ```

### 404 Not Found

1. Проверить routes:
   ```bash
   curl http://localhost:8090/actuator/gateway/routes
   ```

2. Проверить Path предикат:
   ```yaml
   predicates:
     - Path=/api/accounts/**  # Убедитесь что путь правильный
   ```

3. Проверить StripPrefix:
   ```yaml
   filters:
     - StripPrefix=1  # Убирает /api, оставляет /accounts/**
   ```

### CORS ошибки

1. Проверить CORS конфигурацию
2. В production указать конкретные origins
3. Проверить что OPTIONS запросы разрешены

### Медленные запросы

1. Проверить логи (Duration в LoggingFilter)
2. Добавить timeout:
   ```yaml
   spring:
     cloud:
       gateway:
         httpclient:
           connect-timeout: 1000
           response-timeout: 5s
   ```
3. Использовать Circuit Breaker

## Production рекомендации

### 1. CORS настройки

```yaml
allowed-origins:
  - https://mybank.com
  - https://www.mybank.com
# НЕ используйте "*" в production!
```

### 2. Rate Limiting

Защита от DDoS:
```yaml
- name: RequestRateLimiter
  args:
    redis-rate-limiter.replenishRate: 100
    redis-rate-limiter.burstCapacity: 200
```

### 3. Circuit Breaker

Защита от каскадных отказов:
```yaml
resilience4j:
  circuitbreaker:
    configs:
      default:
        failure-rate-threshold: 50
        wait-duration-in-open-state: 10000
```

### 4. Timeouts

```yaml
spring:
  cloud:
    gateway:
      httpclient:
        connect-timeout: 1000
        response-timeout: 5s
```

### 5. Security

- OAuth2 авторизация
- HTTPS only
- API keys для внешних клиентов

### 6. Monitoring

- Prometheus metrics
- Distributed tracing (Zipkin/Jaeger)
- Логирование всех запросов

### 7. Load Balancing

- Несколько инстансов Gateway
- Health checks
- Retry политика

---

**Gateway Service готов!** 🎉

**Порт:** 8090
**Единая точка входа:** http://localhost:8090/api/**
**Status:** ✅ ГОТОВ К ЗАПУСКУ

# Discovery Service - Eureka Server

## Описание

Discovery Service - это сервис регистрации и обнаружения микросервисов на базе **Netflix Eureka Server**.

### Зачем нужен?

В микросервисной архитектуре сервисы должны находить друг друга динамически. Eureka Server решает эту задачу:

1. 📍 **Service Registration** - все микросервисы регистрируются в Eureka при запуске
2. 🔍 **Service Discovery** - сервисы находят друг друга по имени приложения
3. ⚖️ **Load Balancing** - встроенная балансировка нагрузки (client-side)
4. 💚 **Health Checks** - автоматическая проверка состояния сервисов
5. 📊 **Monitoring** - Web Dashboard для визуального мониторинга

## Структура модуля

```
discovery-service/
├── pom.xml
├── src/
│   ├── main/
│   │   ├── java/com/mybank/discovery/
│   │   │   └── DiscoveryServiceApplication.java    # @EnableEurekaServer
│   │   └── resources/
│   │       └── application.yml                     # Конфигурация Eureka
│   └── test/
│       └── java/com/mybank/discovery/
│           └── DiscoveryServiceApplicationTests.java
└── README.md
```

## Технологии

- **Spring Boot 4.0.2**
- **Spring Cloud 2025.1.0**
- **Netflix Eureka Server** - Service Discovery

## Конфигурация

### Port
- **8761** - стандартный порт Eureka Server

### Основные настройки (application.yml)

```yaml
eureka:
  client:
    register-with-eureka: false    # Сам себя не регистрируем
    fetch-registry: false          # Реестр не загружаем
  
  server:
    enable-self-preservation: false  # Отключено для dev
    eviction-interval-timer-in-ms: 60000  # Очистка каждую минуту
```

### Self-Preservation Mode

⚠️ **В production рекомендуется включить!**

```yaml
eureka:
  server:
    enable-self-preservation: true
```

Self-preservation защищает от удаления сервисов при сетевых сбоях.

## Как работает

### 1. Запуск Eureka Server

```bash
mvn spring-boot:run
```

### 2. Регистрация клиентов

Другие сервисы (Accounts, Gateway, etc.) добавляют в свои конфигурации:

```yaml
eureka:
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka/
  instance:
    prefer-ip-address: true
```

### 3. Service Discovery

Клиенты находят друг друга:

```java
// Вместо http://accounts-service:8081
// Можно просто указать имя сервиса
restTemplate.getForObject("http://accounts-service/api/accounts", Account.class);

// Eureka автоматически резолвит имя в URL
```

## Web Dashboard

После запуска откройте в браузере:

**http://localhost:8761**

Dashboard показывает:
- 📋 Список зарегистрированных сервисов
- 🟢 Статус каждого сервиса (UP/DOWN)
- 📊 Количество инстансов каждого сервиса
- ⏱️ Время последнего heartbeat
- 📈 Общую статистику реестра

### Скриншот Dashboard

```
┌─────────────────────────────────────────┐
│         EUREKA DASHBOARD                │
├─────────────────────────────────────────┤
│ Instances currently registered:         │
│                                          │
│ ▶ ACCOUNTS-SERVICE (1)                  │
│   • localhost:accounts-service:8081     │
│     Status: UP                          │
│                                          │
│ ▶ GATEWAY-SERVICE (1)                   │
│   • localhost:gateway-service:8090      │
│     Status: UP                          │
│                                          │
│ ▶ FRONT-UI (1)                          │
│   • localhost:front-ui:8080             │
│     Status: UP                          │
└─────────────────────────────────────────┘
```

## Endpoints

### Eureka Dashboard
- `GET http://localhost:8761` - Web UI

### Eureka API
- `GET http://localhost:8761/eureka/apps` - список всех приложений (XML)
- `GET http://localhost:8761/eureka/apps/{appName}` - информация о приложении
- `POST http://localhost:8761/eureka/apps/{appName}` - регистрация инстанса
- `DELETE http://localhost:8761/eureka/apps/{appName}/{instanceId}` - удаление инстанса

### Actuator
- `GET http://localhost:8761/actuator/health` - health check
- `GET http://localhost:8761/actuator/info` - информация о сервисе
- `GET http://localhost:8761/actuator/metrics` - метрики

## Зависимости

### Основные зависимости в pom.xml:

```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-netflix-eureka-server</artifactId>
</dependency>

<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

## Запуск

### Локально:
```bash
cd discovery-service
mvn spring-boot:run
```

### Из корня проекта:
```bash
mvn spring-boot:run -pl discovery-service
```

### Docker (когда будет Dockerfile):
```bash
docker build -t discovery-service .
docker run -p 8761:8761 discovery-service
```

## Health Check

Проверить что сервис запустился:

```bash
curl http://localhost:8761/actuator/health
```

Ожидаемый ответ:
```json
{
  "status": "UP",
  "components": {
    "diskSpace": { "status": "UP" },
    "ping": { "status": "UP" }
  }
}
```

## Логирование

```yaml
logging:
  level:
    com.netflix.eureka: INFO        # Логи Eureka Server
    com.netflix.discovery: INFO     # Логи Discovery клиента
```

Для отладки можно включить DEBUG:
```yaml
logging:
  level:
    com.netflix.eureka: DEBUG
```

## Особенности работы

### Heartbeat
- Клиенты отправляют heartbeat каждые **30 секунд** (по умолчанию)
- Если heartbeat не приходит **90 секунд** - инстанс помечается как DOWN
- Через **60 секунд** неактивные инстансы удаляются (eviction-interval)

### Кэширование
- Клиенты кэшируют реестр локально
- Обновление кэша каждые **30 секунд**
- Это уменьшает нагрузку на Eureka Server

### Зоны доступности
В production можно настроить несколько зон:

```yaml
eureka:
  client:
    region: eu-west-1
    availability-zones:
      eu-west-1: zone-1,zone-2
```

## Интеграция с другими сервисами

Все остальные сервисы в проекте должны зарегистрироваться в Eureka:

1. ✅ **Gateway Service** - маршрутизирует запросы по именам сервисов
2. ✅ **Accounts Service** - регистрируется как "accounts-service"
3. ✅ **Cash Service** - регистрируется как "cash-service"
4. ✅ **Transfer Service** - регистрируется как "transfer-service"
5. ✅ **Notifications Service** - регистрируется как "notifications-service"
6. ⚠️ **Front UI** - может зарегистрироваться (опционально)
7. ⚠️ **OAuth2 Server** - может зарегистрироваться (опционально)
8. ⚠️ **Config Service** - обычно не регистрируется

## Troubleshooting

### Сервис не появляется в Eureka

1. Проверить что клиент правильно настроен:
```yaml
eureka:
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka/
```

2. Проверить логи клиента - есть ли сообщение о регистрации?

3. Проверить сеть - доступен ли Eureka Server?

### Self-preservation mode активирован

Сообщение в логах:
```
EMERGENCY! EUREKA MAY BE INCORRECTLY CLAIMING INSTANCES ARE UP WHEN THEY'RE NOT.
```

**Причина:** Слишком много инстансов перестали отправлять heartbeat.

**Решение:**
- В dev: отключить `enable-self-preservation: false`
- В production: проверить сеть между сервисами

### Старые инстансы не удаляются

Увеличить частоту eviction:
```yaml
eureka:
  server:
    eviction-interval-timer-in-ms: 30000  # 30 секунд вместо 60
```

## Production рекомендации

1. **Несколько инстансов Eureka** (для отказоустойчивости)
2. **Включить self-preservation mode**
3. **Настроить security** (Basic Auth или OAuth2)
4. **Мониторинг** (Prometheus + Grafana)
5. **Health checks** на всех сервисах

---

**Discovery Service готов!** 🎉

**Порт:** 8761
**Dashboard:** http://localhost:8761
**Status:** ✅ ГОТОВ К ЗАПУСКУ

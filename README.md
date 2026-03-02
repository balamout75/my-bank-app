# 🏦 My Bank — Sprint Project (Yandex Practicum)

# Домашняя работа к одиннадцатому спринту курса Java Middle Developer (Yandex Practicum)

---

# Общая информация

Проект представляет собой **микросервисное приложение «Банк»**, реализованное на базе Spring Boot и Spring Cloud.

Система состоит из следующих сервисов:

| Сервис | Назначение |
|--------|------------|
| **Front UI** | Веб-интерфейс пользователя (Thymeleaf) |
| **Accounts Service** | Аккаунты пользователей и баланс |
| **Cash Service** | Пополнение и снятие средств |
| **Transfer Service** | Переводы между пользователями |
| **Notifications Service** | Уведомления (Kafka consumer + Outbox) |
| **Gateway Service** | API Gateway (Spring Cloud Gateway) |
| **Keycloak** | Аутентификация и роли (OAuth2 / OIDC) |
| **PostgreSQL** | Хранение данных (multi-schema) |
| **Apache Kafka** | Асинхронная доставка событий (KRaft, без ZooKeeper) |

Архитектура соответствует микросервисному подходу. Сервисы взаимодействуют через REST (синхронно) и Apache Kafka (асинхронно — для уведомлений через Outbox-паттерн). Service Discovery и конфигурация обеспечиваются средствами Kubernetes (DNS, ConfigMap, Secret).

---

# 🚀 Варианты развёртывания

| Вариант | Описание | Документация |
|---------|----------|-------------|
| **Kubernetes + Helm** | Основной способ деплоя. K8s DNS, ConfigMap, Ingress-nginx | [k8s/README.md](k8s/README.md) |
| **Jenkins CI/CD** | Автоматизация: тесты → сборка → GHCR → TEST → PROD | [jenkins/README.md](jenkins/README.md) |

---

# 🧭 Личный технический контекст

В данной работе я сознательно вышел из зоны комфорта:
- Проект реализован на **Maven**, без использования Gradle
- Используется **классический Spring MVC**, без WebFlux

После предыдущих проектов на Gradle и WebFlux я ощутил разницу в лаконичности конфигураций, гибкости сборки и выразительности реактивного стиля. Тем не менее, работа позволила глубже понять устройство Spring Boot в более традиционном (servlet) стеке.

---

# 🔐 Безопасность

Используется:
- OAuth 2.0
- OpenID Connect
- Keycloak

### Аутентификация пользователей

Пользователь входит через Keycloak. JWT access token используется для доступа к сервисам.

### Межсервисное взаимодействие

Сервисы взаимодействуют через **client_credentials** flow.  
Каждый сервис выступает как OAuth2 client и Resource Server одновременно.

### Типы токенов

| Кто | Какой токен |
|-----|------------|
| Пользователь | Authorization Code Flow |
| Сервис ↔ сервис | Client Credentials Flow |

---

# 📦 Сервисы

## 🧑 Accounts Service

| Метод | URL | Описание |
|------|-----|----------|
| GET | `/accounts/me` | Профиль пользователя |
| PUT | `/accounts/me` | Обновление профиля |
| GET | `/accounts/all` | Список других пользователей |
| POST | `/accounts/balance` | Изменение баланса (внутренний) |
| POST | `/accounts/transfer` | Обработка перевода (consume) |

---

## 💰 Cash Service

| Метод | URL | Описание |
|------|-----|----------|
| GET | `/cash/operation-key` | Получить idempotency ключ |
| POST | `/cash/operate` | Пополнение / снятие |

---

## 🔁 Transfer Service

| Метод | URL | Описание |
|------|-----|----------|
| GET | `/transfer/operation-key` | Получить idempotency ключ |
| POST | `/transfer/transfer` | Выполнить перевод |

---

## 🔔 Notifications Service

| Метод | Источник | Описание |
|------|----------|----------|
| Kafka topic `notifications` | Cash / Transfer / Accounts | Приём события через Kafka consumer |

Особенности:
- Kafka consumer с at-least-once гарантией
- Idempotency через `(service, operation_id)` — дубли игнорируются
- OutboxProcessor: периодическая отправка уведомлений со статусом `PENDING`
- Хранение JSON payload

---

# 👤 Демонстрационные учетные записи

Для проверки работы системы в Keycloak заведены пользователи:

| Пользователь | Логин     | Пароль |
|--------------|-----------|--------|
| Клиент 1 | **Alice** | alice |
| Клиент 2 | **Bob**  | bob |

Эти учетные записи используются для проверки переводов, операций с балансом и работы межсервисных сценариев.

---

# 🚀 Запуск проекта (Kubernetes + Helm)

### Шаг 1. Сборка Docker-образов

Проект использует **Docker BuildKit + multi-stage сборку**:

✔ Maven на компьютере **НЕ нужен**  
✔ Всё собирается внутри Docker

```bash
docker buildx bake --load -f docker-bake.hcl
```

### Шаг 2. Подготовка секретов

```bash
cp k8s/values-local.yaml.example k8s/values-local.yaml
```

Заполните `values-local.yaml` своими значениями (пароли, client secrets).

### Шаг 3. Деплой

```bash
helm dependency update k8s
helm upgrade --install mybank k8s -n mybank --create-namespace -f k8s/values-local.yaml
```

### Шаг 4. Доступ

Добавьте в `/etc/hosts` (или `C:\Windows\System32\drivers\etc\hosts`):

```
127.0.0.1 mybank.dev.local keycloak.mybank.dev.local
```

Приложение будет доступно:

```
http://mybank.dev.local
```

Подробная инструкция: **[k8s/README.md](k8s/README.md)**

---

# 🚀 Jenkins CI/CD

Полная автоматизация: тесты → сборка → push образов в GHCR → деплой в TEST → ручное подтверждение → деплой в PROD.

Подробная инструкция: **[jenkins/README.md](jenkins/README.md)**

---

# 🏗 Стек

- Spring Boot 3
- Spring Cloud Gateway
- Apache Kafka 4.2 (KRaft, без ZooKeeper)
- Keycloak (SSO, OAuth2 / OIDC)
- PostgreSQL (multi-schema)
- Docker + BuildKit
- Kubernetes + Helm + Ingress-nginx
- Jenkins CI/CD (GHCR)
- JUnit 5, Mockito, Testcontainers, Spring Cloud Contract
- Resilience4j (Circuit Breaker, Retry)
- Liquibase (миграции БД)

---

# 🏗 Архитектура (схема)

```text
                   ┌────────────────────────────────────────────┐
                   │                 Browser / User             │
                   └──────────────────────────┬─────────────────┘
                                              │
                                              ▼
                                     ┌─────────────────┐
                                     │  Ingress-nginx  │
                                     │  (K8s Ingress)  │
                                     └───────┬─────────┘
                                             │
                          ┌──────────────────┼──────────────────┐
                          │ /mybank/*        │                  │ /keycloak/*
                          ▼                  │                  ▼
                  ┌─────────────────┐        │         ┌──────────────┐
                  │     Front UI    │        │         │   Keycloak   │
                  │  (web, 8081)    │        │         │  (SSO/OIDC)  │
                  └───────┬─────────┘        │         └──────────────┘
                          │ REST calls (JWT) │
                          ▼                  │
                  ┌─────────────────┐        │
                  │ Gateway Service │        │
                  │  (edge API)     │        │
                  └───────┬─────────┘        │
                          │ routes by K8s DNS │
        ┌─────────────────┼─────────────────┐
        │                 │                 │
        ▼                 ▼                 ▼
┌──────────────┐  ┌─────────────┐  ┌─────────────────┐
│   Accounts   │  │    Cash     │  │    Transfer     │
│   Service    │  │   Service   │  │    Service      │
└──────┬───────┘  └──────┬──────┘  └───────┬─────────┘
       │                 │                 │
       │  REST (client_credentials JWT)    │
       └────────┬────────┴────────┬────────┘
                │                 │
                │  ┌──────────┐  │
                ├─▶│  Kafka   │◀─┤  Outbox → Kafka producer
                │  │ (KRaft)  │  │
                │  └────┬─────┘  │
                │       │        │
                │       ▼        │
                │ ┌────────────┐ │
                │ │Notifications│ │
                │ │  Service   │ │  Kafka consumer
                │ └────────────┘ │
                │                │
                ▼                │
       ┌─────────────────────┐   │
       │     PostgreSQL      │◀──┘
       │ schemas: accounts / │
       │ cash / transfer /   │
       │ notifications       │
       └─────────────────────┘
```

Ключевые принципы:
- внешние запросы идут через **Ingress-nginx → Front UI → Gateway**
- Service Discovery — через **Kubernetes DNS** (без Eureka)
- конфигурация — через **ConfigMap / Secret** (без Config Server)
- безопасность обеспечивается **Keycloak** (JWT, roles)
- асинхронные уведомления — **Outbox → Kafka → Notifications Service**
- изоляция данных — через **схемы PostgreSQL** (по сервису)

---

# 🔄 Flow операций

Ниже приведены два основных бизнес-сценария на уровне API и межсервисных вызовов.

## 1) Пополнение / снятие (deposit / withdraw)

**Цель:** изменить баланс пользователя и зафиксировать операцию с защитой от повторов (idempotency).

### Шаги

1. **Получение ключа операции (idempotency key)**
   - Клиент вызывает:
      - `GET /cash/operation-key`
   - В ответ получает `operationId` (ключ операции).

2. **Выполнение операции пополнения/снятия**
   - Клиент вызывает:
      - `POST /cash/operate`
   - Тело запроса содержит `operationId` и параметры операции (тип, сумма и т.п.).
   - Cash Service сохраняет операцию в своей схеме БД и инициирует изменение баланса.

3. **Изменение баланса в Accounts Service**
   - Cash Service вызывает **Accounts Service** (внутренний endpoint):
      - `POST /accounts/balance`
   - Вызов выполняется с сервисным JWT (client_credentials).
   - Accounts Service применяет изменение баланса с учётом идемпотентности (через `service_operations`).

4. **Формирование уведомления (через Kafka)**
   - Cash Service записывает событие в таблицу Outbox.
   - OutboxProcessor периодически читает записи со статусом `PENDING` и отправляет их в Kafka topic `notifications`.
   - Notifications Service (Kafka consumer) получает событие и сохраняет уведомление в БД.

### Идемпотентность

- ключ операции создаётся заранее (`GET /cash/operation-key`)
- повторный вызов с тем же `(service, operation_id)` не приводит к двойному списанию/зачислению
- Notifications также защищён от дублей составным ключом
- Kafka consumer обеспечивает at-least-once + idempotent processing

---

## 2) Перевод между пользователями (transfer)

**Цель:** списать средства у отправителя и зачислить получателю, зафиксировать операцию и уведомления.

### Шаги

1. **Получение ключа операции**
   - Клиент вызывает:
      - `GET /transfer/operation-key`
   - Получает `operationId`.

2. **Создание операции перевода**
   - Клиент вызывает:
      - `POST /transfer/transfer`
   - Transfer Service сохраняет операцию перевода в своей схеме БД и инициирует изменения балансов.

3. **Изменение балансов (межсервисно)**
   - Transfer Service вызывает Accounts Service (внутренний endpoint):
      - `POST /accounts/transfer`
   - Вызов идёт с сервисным JWT (client_credentials).
   - Accounts Service выполняет списание/зачисление и фиксирует факт применения (idempotency).

4. **Уведомление (через Kafka)**
   - Transfer Service записывает событие в таблицу Outbox.
   - OutboxProcessor отправляет событие в Kafka topic `notifications`.
   - Notifications Service получает и сохраняет уведомление.

### Идемпотентность

- повторный запрос с тем же `operationId` считается дублем
- ключ уникальности: `(service, operation_id)`
- это позволяет безопасно применять Retry при ошибках сети/временной недоступности

---

## Важно про SAGA (перспектива развития)

Перевод денег естественным образом укладывается в паттерн **SAGA**.  
В текущей версии перевод выглядит как единая цепочка вызовов, однако в следующих итерациях логично:
- разделить перевод на 2 независимых шага (debit/credit)
- добавить компенсации
- реализовать хореографическую SAGA для устойчивости распределённой транзакции

---

# 🧠 Реализация и опыт

### Основные технические решения

#### Idempotency
Используется составной ключ `(service, operation_id)` для защиты от повторной обработки.

#### Outbox → Kafka
Сервисы-инициаторы (Cash, Transfer, Accounts) записывают события в таблицу Outbox. OutboxProcessor периодически отправляет их в Kafka. Notifications Service подписан на topic `notifications` и сохраняет уведомления идемпотентно. При успешной обработке Outbox-запись переходит в статус `NOTIFIED`.

#### OAuth2 на уровне сервисов
Каждый сервис — Resource Server + OAuth2 Client.

#### Circuit Breaker и Retry
Используется Resilience4j для устойчивости межсервисных вызовов.

#### Multi-schema Postgres
Каждый сервис использует собственную схему:
```
accounts
cash
transfer
notifications
```

#### Kubernetes-native конфигурация
Вместо Spring Cloud Config Server и Eureka используются ConfigMap, Secret и K8s DNS. Каждый сервис получает настройки из ConfigMap, а секреты (client secrets, пароли БД) — из Secret-ов.

---

# 🧪 Тестирование

В проекте реализовано многоуровневое тестирование: модульное, интеграционное, контрактное (provider + consumer) и Kafka-интеграционное. Общее количество тестов — **55+**. Подход ориентирован на проверку архитектурных свойств сервисов: безопасности, идемпотентности, Kafka-пайплайна, REST-контрактов и устойчивости взаимодействия.

## 🧱 Общая стратегия

| Уровень | Назначение | Стек |
|--------|------------|------|
| Unit tests | Бизнес-логика без Spring-контекста | JUnit 5, Mockito |
| Integration tests | Контроллеры, БД, безопасность, бизнес-флоу | Spring Boot Test, MockMvc, Testcontainers, Liquibase |
| Kafka IT | Проверка Outbox → Kafka → Consumer пайплайна | @EmbeddedKafka, Testcontainers |
| Contract tests (provider) | Гарантия неизменности API сервиса | Spring Cloud Contract Verifier |
| Contract tests (consumer) | Проверка клиентов против опубликованных стабов | Spring Cloud Contract Stub Runner |

---

## 💰 Cash Service (Microservice Chassis)

Cash Service выступает как типовой microservice chassis: REST API, Security (OAuth2 Resource Server), Idempotency, работа с БД, Outbox → Kafka, вызовы других сервисов, error handling. Поэтому сервис покрыт тестами максимально глубоко.

### Unit тесты — CashServiceTest

Чистый Mockito (`@ExtendWith(MockitoExtension.class)`), без Spring-контекста.

| Тест | Что проверяет |
|------|---------------|
| operate_deposit_success | Создание операции, вызов AccountsClient, создание Outbox-записи |
| operate_statusUpdate | Изменение статуса операции при успехе/ошибке |
| operate_callsNotifications | Формирование события для Kafka |

Моки: `CashOperationRepository`, `AccountsClient`, `OutboxRepository`.

### Интеграционные тесты — CashControllerIT

`@SpringBootTest` + `MockMvc` + Testcontainers (PostgreSQL) + Liquibase. Безопасность **не отключена** — используется эмуляция JWT:

```java
var auth = jwt().jwt(j -> j
        .claim("preferred_username", "alice")
        .claim("clientRoles", "cash.write")
).authorities(new SimpleGrantedAuthority("ROLE_cash.write"));
```

| Тест | Что проверяет |
|------|---------------|
| operate_success_shouldReturn204 | Успешное выполнение операции |
| flow_reserveKey_thenOperate | Полный бизнес-флоу (ключ → операция) |
| operate_twice_shouldReturn400or409 | Идемпотентность (повторный вызов = ошибка) |
| operate_withoutWriteRole_shouldReturn403 | RBAC: запрет без роли `cash.write` |

### Kafka IT — OutboxKafkaIT

`@EmbeddedKafka` + Testcontainers (PostgreSQL). Проверяет полный пайплайн: создание Outbox-записи → OutboxProcessor отправляет в Kafka → consumer получает событие. Используется `poll + filter по operationId` для устранения race condition.

### Контрактные тесты (provider)

Spring Cloud Contract Verifier генерирует тесты из контрактов в `src/test/resources/contracts/`:

| Контракт | Проверка |
|----------|---------|
| GET /cash/operation-key | → 200 OK, JSON с `operationId` |
| POST /cash/operate | → 204 No Content при корректном запросе |
| POST /cash/operate (invalid) | → 400 Bad Request, стандартная структура ошибки |

---

## 🔁 Transfer Service

### Unit тесты — TransferServiceTest

Аналогично Cash Service: чистый Mockito, проверка создания операции, вызова AccountsClient, формирования Outbox-записи.

### Интеграционные тесты — TransferControllerIT

`@SpringBootTest` + `MockMvc` + Testcontainers + JWT-эмуляция. Проверяет полный флоу перевода, идемпотентность, RBAC.

### Kafka IT — OutboxKafkaIT

`@EmbeddedKafka` + Testcontainers. Outbox → Kafka producer → consumer. Poll + filter по `operationId`.

---

## 🧑 Accounts Service

### Unit тесты — AccountsServiceTest

Чистый Mockito. Проверка создания/обновления аккаунта, изменения баланса, применения transfer-операции.

### Kafka IT — OutboxKafkaIT

`@EmbeddedKafka` + Testcontainers. Проверяет Outbox → Kafka для событий accounts-service.

### Контрактные тесты (provider)

`ContractTestBase` с `@MockitoBean` — мокирует репозиторий, исключает DataSource/Liquibase/Kafka из контекста через `application-contract-test.yml`.

| Контракт | Проверка |
|----------|---------|
| GET /accounts/me | → 200 OK, JSON с профилем пользователя |
| GET /accounts/all | → 200 OK, массив пользователей |
| PUT /accounts/me | → 200 OK, обновлённый профиль |

---

## 🔔 Notifications Service

### Unit тесты (7 тестов)

**NotificationServiceTest** (2 теста) — чистый Mockito:

| Тест | Что проверяет |
|------|---------------|
| createFromEvent_new | Создание уведомления из нового события |
| createFromEvent_duplicate | Игнорирование дубликата по `(service, operationId)` |

**OutboxProcessorTest** (2 теста) — чистый Mockito + `ReflectionTestUtils.setField` (для `@Value`):

| Тест | Что проверяет |
|------|---------------|
| sendNotification | Формирование и отправка одного уведомления |
| process | Обработка пакета Outbox-записей со статусом `PENDING` |

**NotificationKafkaListenerTest** (3 теста) — чистый Mockito (`@ExtendWith(MockitoExtension.class)`):

| Тест | Что проверяет |
|------|---------------|
| onEvent_new | Приём Kafka-события → вызов `NotificationService.createFromEvent` |
| onEvent_duplicate | Дублирующее событие → игнорируется |
| onEvent_error | Ошибка → исключение пробрасывается (retry через Kafka) |

### Интеграционные тесты — NotificationKafkaListenerIT (2 теста)

`@SpringBootTest` + `@EmbeddedKafka` + Testcontainers (PostgreSQL) + Liquibase + профиль `kafka-test`.

| Тест | Что проверяет |
|------|---------------|
| shouldConsumeEventAndSaveNotification | Kafka событие → запись в БД |
| shouldHandleDuplicateEvents | Повторное событие → идемпотентность (одна запись в БД) |

---

## 🖥 Front UI

### Unit тесты — DashboardServiceTest (5 тестов)

Чистый Mockito. Проверяет агрегацию данных из CashClient, TransferClient, AccountsClient для отображения на дашборде.

### Интеграционные тесты — FrontControllerIT (8 тестов)

`@SpringBootTest` с OAuth2 Login эмуляцией. Проверяет маршруты контроллера, редиректы, доступ к защищённым страницам.

### Consumer контрактные тесты (3 теста)

Проверяют, что REST-клиенты Front UI корректно работают с API backend-сервисов. Используют опубликованные стабы через Spring Cloud Contract Stub Runner.

Оптимизация: дублирование кода устранено через общий `BaseClientConsumerTest.clientFor()`.

| Тест | Что проверяет |
|------|---------------|
| CashClientConsumerTest | Front UI → Cash Service (operation-key, operate) |
| TransferClientConsumerTest | Front UI → Transfer Service (operation-key, transfer) |
| AccountsClientConsumerTest | Front UI → Accounts Service (me, all, update) |

---

## 🔐 Подход к Security в тестах

Тесты **не отключают безопасность**, а эмулируют JWT, что позволяет проверять RBAC:

```java
var auth = jwt().jwt(j -> j
        .claim("preferred_username", "alice")
        .claim("clientRoles", "cash.write")
).authorities(new SimpleGrantedAuthority("ROLE_cash.write"));
```

Проверяется: доступ с правильной ролью, запрет без роли (403), корректная работа `@AuthenticationPrincipal Jwt`.

## 🗄 Подход к БД в тестах

Каждый интеграционный тест поднимает PostgreSQL через Testcontainers (JDBC URL в `application.yml`), накатывает схему через Liquibase и использует реальный JPA-репозиторий. Это гарантирует, что SQL-запросы и миграции работают на реальной БД, а не на H2.

## 📊 Сводная таблица тестов

| Сервис | Unit | Integration | Kafka IT | Contract (provider) | Contract (consumer) | Итого |
|--------|------|-------------|----------|--------------------|--------------------|-------|
| Cash Service | 3+ | 4 | 1 | 3 | — | 11+ |
| Transfer Service | 3+ | 4 | 1 | — | — | 8+ |
| Accounts Service | 3+ | — | 1 | 3 | — | 7+ |
| Notifications Service | 7 | 2 | — | — | — | 9 |
| Front UI | 5 | 8 | — | — | 3 | 16 |
| **Итого** | **21+** | **18+** | **3** | **6** | **3** | **51+** |

---

# 📌 Итоги

Проект позволил на практике разобраться с:
- OAuth2 / JWT
- Keycloak (SSO в Kubernetes)
- Outbox-паттерном → Kafka
- Apache Kafka (KRaft, producer/consumer, at-least-once)
- Idempotency
- Resilience4j
- Liquibase
- Микросервисной архитектурой
- Kubernetes и Helm (ConfigMap, Secret, Ingress, StatefulSet)
- CI/CD с Jenkins (GHCR, parallel builds, Helm deploy)
- Testcontainers + EmbeddedKafka
- Spring Cloud Contract (provider + consumer)

---

# 🚧 Цели, которые стали очевидны, но не были доведены до реализации

Работа над проектом позволила выявить направления, требующие дальнейшего развития.

### 1. Transfer Service как SAGA
Сервис переводов по своей природе является хорошим кандидатом для реализации паттерна **SAGA**.  
Текущая реализация опирается на централизованную логику, однако в следующей работе планируется:
- разделить перевод на две независимые транзакции
- реализовать **хореографическую SAGA**
- добиться устойчивости распределенной транзакции без блокировок

---

### 2. Обработка кастомных исключений
Механизм проброса бизнес-исключений от уровня сервисов до пользователя реализован частично.  
В ряде сценариев можно было:
- дать более точные сообщения
- сохранить больше диагностической информации
- стандартизировать error response

Этот аспект планируется улучшить в следующих проектах.

### 3. openAPI
Очень пожалел, что не реализовал, постараюсь заняться этим при следуещем возврате к коду. Прямо сильно не хватало плана работы. Причина банальна, набрал много нового, и просто не знал, как оно получится. В итоге экспромт стал вдвое сложнее обдуманной разработки.

---

# 📁 Структура проекта

```
my-bank-app/
├── accounts-service/           # Микросервис управления счетами
├── cash-service/               # Микросервис операций с наличными
├── transfer-service/           # Микросервис переводов
├── notifications-service/      # Микросервис уведомлений (Kafka consumer)
├── gateway-service/            # API Gateway
├── front-ui/                   # Веб-интерфейс (Thymeleaf)
├── keycloak/                   # Realm export
├── k8s/                        # Kubernetes Helm чарты
├── jenkins/                    # CI/CD (Jenkins + Dockerfile)
├── docker-bake.hcl             # Multi-target Docker build
├── Dockerfile.build            # Multi-stage Dockerfile (локальная сборка)
├── Dockerfile.ci               # Lightweight Dockerfile (Jenkins CI)
├── pom.xml                     # Корневой Maven POM
├── TECH_DEBT.md                # Технический долг
└── README.md                   # Этот файл
```

# Автор

Иван Васильев

# 🏦 My Bank — Sprint Project (Yandex Practicum)

# Домашняя работа к десятому спринту курса Java Middle Developer (Yandex Practicum)

---

# Общая информация

Проект представляет собой **микросервисное приложение «Банк»**, реализованное на базе Spring Boot и Spring Cloud.

Система состоит из следующих сервисов:

| Сервис | Назначение |
|--------|------------|
| **Front UI** | Веб-интерфейс пользователя |
| **Accounts Service** | Аккаунты пользователей и баланс |
| **Cash Service** | Пополнение и снятие средств |
| **Transfer Service** | Переводы между пользователями |
| **Notifications Service** | Уведомления (Outbox / Event Store) |
| **Gateway** | API Gateway |
| **Config Server** | Централизованная конфигурация (Docker Compose) |
| **Eureka** | Service Discovery (Docker Compose) |
| **Keycloak** | Аутентификация и роли |
| **PostgreSQL** | Хранение данных |

Архитектура соответствует микросервисному подходу и использует REST‑взаимодействие между сервисами.

---

# 🚀 Варианты развёртывания

Проект поддерживает три варианта развёртывания:

| Вариант | Описание | Документация |
|---------|----------|-------------|
| **Docker Compose** | Локальная разработка с Eureka и Config Server | Этот файл (ниже) |
| **Kubernetes + Helm** | Деплой в K8s без Eureka (K8s DNS + ConfigMap) | [k8s/README.md](k8s/README.md) |
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
- Keycloak (Docker)

### Аутентификация пользователей

Пользователь входит через Keycloak. JWT access token используется для доступа к сервисам.

### Межсервисное взаимодействие

Сервисы взаимодействуют через **client_credentials** flow.  
Каждый сервис выступает как OAuth2 client и Resource Server одновременно.

---

# 🐳 Docker-стенд

Поднимаются контейнеры (в порядке последовательности поднятия):

```
postgres
keycloak
eureka
config-server
gateway
notifications-service
accounts-service
cash-service
transfer-service
frontend
nginx
ngrok
```

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

| Метод | URL | Описание |
|------|-----|----------|
| POST | `/notifications` | Создать уведомление |

Особенности:
- Ответ: **202 Accepted**
- Idempotency через `(service, operation_id)`
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

# ⚙ Сборка Docker-образов (ОБЯЗАТЕЛЬНЫЙ ШАГ ДЛЯ ВАРИАНТА 1)

Проект использует **Docker BuildKit + multi-stage сборку**, поэтому:

✔ Maven на компьютере **НЕ нужен**  
✔ Всё собирается внутри Docker

В корне проекта выполнить:

```bash
docker buildx bake --load -f docker-bake.hcl
```
✔ Примечание, в девятой работе я допустил неточность, возможно, сказавшуюся на развертывании проекта, приношу извинение, исправил

---

# 🚀 ВАРИАНТ 1. Запуск проекта (через Docker Compose)

### Шаг 1. Подготовка секретов

```bash
cp docker-compose.env.example docker-compose.env
```

Заполните `docker-compose.env` своими значениями.

### Шаг 2. Запуск

```bash
docker compose --env-file docker-compose.env up -d
```

Приложение будет доступно:

```
http://localhost:8081
```

---

# 🚀 ВАРИАНТ 2. Запуск проекта ВРУЧНУЮ (без общей docker-compose сети)

Используется для разработки и отладки отдельных сервисов.

## 1️⃣ База данных. В директории etc выполнить команду

```bash
docker run --name yp-database --rm --env-file postgres.env -p 5432:5432 postgres:18.1
```

## 2️⃣ Keycloak. В директории KeycloakContainer выполнить команду

```bash
docker compose up -d --build
```

---

## 3️⃣ Далее запускать сервисы локально из IDE в порядке:

1. **Discovery-service**
2. **Config-service**
3. **Gateway-service**

Дальнейший порядок не важен:
- Notifications-service
- Accounts-service
- Cash-service
- Transfer-service
- Front-ui

---

📌 **Примечание**
- Контейнеры **nginx** и **ngrok** при локальном запуске сервисов **не требуются**
- Доступ к приложению остаётся:

```
http://localhost:8081
```

---

# 🚀 ВАРИАНТ 3. Kubernetes + Helm

Деплой в Kubernetes без Eureka и Config Server. Используются K8s DNS, ConfigMap, Ingress-nginx.

Подробная инструкция: **[k8s/README.md](k8s/README.md)**

---

# 🚀 ВАРИАНТ 4. Jenkins CI/CD

Полная автоматизация: тесты → сборка → push образов в GHCR → деплой в TEST → ручное подтверждение → деплой в PROD.

Подробная инструкция: **[jenkins/README.md](jenkins/README.md)**

---

# 🔐 SSO (Keycloak)

Проект использует **Single Sign-On через Keycloak**.

### Страница входа

При открытии:

```
http://localhost:8081
```

пользователь автоматически перенаправляется на страницу логина Keycloak.

### После входа

Keycloak выдаёт JWT токен, который используется:
- Gateway
- Accounts Service
- Cash Service
- Transfer Service
- Notifications Service

### Типы токенов

| Кто | Какой токен |
|-----|------------|
| Пользователь | Authorization Code Flow |
| Сервис ↔ сервис | Client Credentials Flow |

---

# 🧠 Что происходит под капотом

| Этап | Где выполняется |
|------|----------------|
| Maven сборка | внутри Docker builder stage |
| Кэш зависимостей Maven | через BuildKit cache mount |
| Финальный образ | лёгкий JRE runtime |
| Локальный Maven | **не требуется** |

---

# 🧹 ВАЖНО: .gitignore

```
.buildx-cache/
.buildx-cache/blobs/
.buildx-cache/index.json
.buildx-cache/ingest/
docker-compose.env
jenkins/.env
```

---

# 🏗 Стек

- Spring Boot Microservices
- Spring Cloud (Gateway, Config, Eureka)
- Keycloak (SSO)
- PostgreSQL
- Docker + BuildKit
- Kubernetes + Helm
- Jenkins CI/CD
- Nginx + ngrok

---

# 🏗 Архитектура (схема)

Ниже — упрощённая схема взаимодействия компонентов в docker-стенде:

```text
                   ┌────────────────────────────────────────────┐
                   │                 Browser / User             │
                   └──────────────────────────┬─────────────────┘
                                              │
                                              ▼
                                     ┌─────────────────┐
                                     │    Nginx (gw)   │
                                     │  reverse proxy  │
                                     └───────┬─────────┘
                                             │  /mybank/*
                                             ▼
                                     ┌─────────────────┐
                                     │     Front UI    │
                                     │  (web, 8081)    │
                                     └───────┬─────────┘
                                             │ REST calls (JWT)
                                             ▼
                                     ┌─────────────────┐
                                     │ Gateway Service │
                                     │  (edge API)     │
                                     └───────┬─────────┘
                                             │ routes by service
                   ┌─────────────────────────┼─────────────────────────┐
                   │                         │                         │
                   ▼                         ▼                         ▼
          ┌──────────────────┐       ┌─────────────────┐       ┌─────────────────┐
          │ Accounts Service │       │   Cash Service  │       │ Transfer Service│
          │ /accounts/*      │       │ /cash/*         │       │ /transfer/*     │
          └───────┬──────────┘       └──────┬──────────┘       └───────┬─────────┘
                  │                         │                          │
                  │ (service-to-service)    │ (service-to-service)     │ (service-to-service)
                  │ OAuth2 client_credentials (JWT, roles)             │
                  └───────────────┬─────────┴─────────┬────────────────┘
                                  ▼                   ▼
                         ┌─────────────────┐  ┌──────────────────────────┐
                         │ Notifications   │  │ PostgreSQL               │
                         │ Service         │  │ schemas: accounts/cash/  │
                         │ /notifications  │  │ transfer/notifications   │
                         └─────────────────┘  └──────────────────────────┘

      ┌──────────────────────────────┐
      │ Eureka (discovery)           │  ← service registration
      └──────────────────────────────┘
      ┌──────────────────────────────┐
      │ Config Server                │  ← centralized config
      └──────────────────────────────┘
      ┌──────────────────────────────┐
      │ Keycloak                     │  ← OAuth2/OIDC issuer (JWT)
      └──────────────────────────────┘
      ┌──────────────────────────────┐
      │ Ngrok                        │  ← public domain → Nginx (для авторизации в Docker)
      └──────────────────────────────┘
```

Ключевые принципы:
- внешние запросы идут через **Nginx → Front UI → Gateway**
- сервисы регистрируются в **Eureka** и получают конфигурацию из **Config Server**
- безопасность обеспечивается **Keycloak** (JWT, roles)
- асинхронные уведомления реализованы через **Notifications Service (Outbox)**
- изоляция данных — через **схемы PostgreSQL** (по сервису)

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
    - Accounts Service применяет изменение баланса с учётом идемпотентности (например, через `service_operations`).

4. **Формирование уведомления**
    - Сервис инициатор (Cash) вызывает:
        - `POST /notifications`
    - В Notifications Service создаётся запись (Outbox) с ключом `(service, operation_id)` и JSON payload.

### Идемпотентность

- ключ операции создаётся заранее `/cash/operation-key`)
- повторный вызов с тем же `(service, operation_id)` не должен приводить к двойному списанию/зачислению
- Notifications также защищён от дублей составным ключом

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

4. **Уведомление**
    - Сервис инициатор (Transfer) вызывает:
        - `POST /notifications`
    - В Outbox сохраняется JSON payload и выставляется статус доставки.

### Идемпотентность

- повторный запрос с тем же `operationId` должен считаться дублем
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

#### Notifications как Outbox
Сервис уведомлений хранит события, статус доставки, попытки и ошибки.

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

#### Docker и multi-module сборка
Настройка сборки multi-module Maven проекта внутри Docker с кешированием зависимостей.

---

# 🧪 Тестирование

В проекте реализовано модульное, интеграционное и контрактное тестирование, отражающее реальные сценарии работы микросервисной системы. Подход ориентирован на проверку архитектурных свойств сервисов: безопасности, идемпотентности, REST‑контрактов и устойчивости взаимодействия.

## 🧱 Общая стратегия

| Уровень | Назначение |
|--------|------------|
| Unit tests | Проверка бизнес-логики без Spring-контекста |
| Integration tests | Контроллеры, БД, безопасность, бизнес-флоу |
| Contract tests | Гарантия неизменности API между сервисами |

Используемый стек: JUnit 5, Mockito, Spring Boot Test, MockMvc, spring-security-test, Testcontainers, Liquibase, Spring Cloud Contract.

---

# 💰 Cash Service как Microservice Chassis

Cash Service выступает как типовой microservice chassis: REST API, Security (OAuth2 Resource Server), Idempotency, работа с БД, вызовы других сервисов, error handling. Поэтому сервис покрыт тестами максимально глубоко.

---

## 🧪 Unit тесты (Cash Service, Transfer Service)

Проверяется изолированная логика:
- создание операции
- изменение статуса операции
- вызовы AccountsClient и NotificationsClient
- обработка ошибок

Моки: CashOperationRepository, AccountsClient, NotificationsClient, а также их клоны из Transfer Service.

---

## 🔐 Security-aware интеграционные тесты

Тесты не отключают безопасность, а эмулируют JWT, что позволяет проверять RBAC.

```java
var auth = jwt().jwt(j -> j
        .claim("preferred_username", "alice")
        .claim("clientRoles", "cash.write")
).authorities(new SimpleGrantedAuthority("ROLE_cash.write"));
```

Проверяется:
- доступ с правильной ролью
- запрет без роли (403)
- корректная работа @AuthenticationPrincipal Jwt

---

## 🗄 Работа с БД в тестах

Каждый интеграционный тест поднимает PostgreSQL через Testcontainers, накатывает схему через Liquibase и использует реальный JPA-репозиторий.

---

## 🔄 Интеграционные сценарии Cash Service

| Тест | Что проверяет |
|------|----------------|
| operate_success_shouldReturn204 | Успешное выполнение операции |
| flow_reserveKey_thenOperate | Полный бизнес-флоу |
| operate_twice_shouldReturn400or409 | Идемпотентность |
| operate_withoutWriteRole_shouldReturn403 | RBAC запрет |

---

# 📜 Контрактные тесты

Контрактные тесты реализованы с использованием Spring Cloud Contract.

### Назначение

Контракты фиксируют HTTP метод, URL, структуру запроса и ответа, HTTP статус. Это гарантирует, что изменения Cash Service не сломают другие сервисы.

### Контракты Cash Service

1. GET /cash/operation-key → 200 OK, JSON с operationId
2. POST /cash/operate → 204 No Content при корректном запросе
3. Ошибка валидации → 400 Bad Request и стандартная структура ошибки

---

## 🎯 Итог

Тестирование Cash Service покрывает бизнес-логику, безопасность, работу с БД, REST-контракты, идемпотентность и устойчивость архитектуры. Проверяются не только методы, но и инженерные принципы микросервисной системы.

---

# 📌 Итоги

Проект позволил на практике разобраться с:
- OAuth2 / JWT
- Keycloak в Docker-сети
- Outbox-паттерном
- Idempotency
- Resilience4j
- Liquibase
- Микросервисной архитектурой
- Kubernetes и Helm
- CI/CD с Jenkins

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
├── notifications-service/      # Микросервис уведомлений
├── gateway-service/            # API Gateway
├── front-ui/                   # Веб-интерфейс (Thymeleaf)
├── config-service/             # Spring Cloud Config Server
├── discovery-service/          # Eureka Server
├── keycloak/                   # Realm export
├── proxy/                      # Nginx config
├── docker-compose.yml          # Вариант 1: Docker Compose
├── docker-compose.env.example  # Шаблон секретов
├── docker-bake.hcl             # Multi-target Docker build
├── Dockerfile.build            # Multi-stage Dockerfile
├── pom.xml                     # Корневой Maven POM
├── k8s/                        # Вариант 3: Kubernetes Helm чарты
├── jenkins/                    # Вариант 4: CI/CD
├── TECH_DEBT.md                # Технический долг
└── README.md                   # Этот файл
```

# Автор

Иван Васильев

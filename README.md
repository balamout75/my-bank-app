# 🏦 MyBank — Микросервисное банковское приложение

> Учебный проект курса **Java Middle Developer** (Yandex Practicum, Sprint 11)

---

## Что это такое

**MyBank** — полноценное микросервисное приложение, реализующее базовые банковские операции: пополнение счёта, переводы между пользователями, уведомления. Проект охватывает весь путь от написания кода до деплоя в Kubernetes с автоматизированным CI/CD и полным observability-стеком.

---

## Сервисы

| Сервис | Порт | Назначение |
|--------|------|------------|
| **front-ui** | 8081 | Веб-интерфейс пользователя (Thymeleaf) |
| **gateway-service** | 8090 | API Gateway (Spring Cloud Gateway) |
| **accounts-service** | 8080 | Управление аккаунтами и балансом |
| **cash-service** | 8080 | Пополнение и снятие средств |
| **transfer-service** | 8080 | Переводы между пользователями |
| **notifications-service** | 8080 | Уведомления (Kafka consumer + Outbox) |
| **Keycloak** | 8080 | OAuth2 / OIDC (SSO) |
| **PostgreSQL** | 5432 | Хранение данных (multi-schema) |
| **Apache Kafka** | 9092 | Асинхронная доставка событий (KRaft) |
| **Zipkin** | 9411 | Распределённый трейсинг |

---

## Архитектура

```
                         Browser / User
                               │
                        Ingress-nginx
                      ┌────────┴────────┐
                      │                 │
               mybank.dev.local  keycloak.mybank.dev.local
                      │                 │
                   Front UI          Keycloak
                      │             (OAuth2/OIDC)
                Gateway Service
           ┌──────────┼──────────┐
           │          │          │
      Accounts     Cash       Transfer
      Service      Service    Service
           │          │          │
           └──────────┴──────────┘
                      │ Kafka (notifications topic)
               Notifications Service
                      │
              PostgreSQL (multi-schema)
              Kafka (KRaft)
```

**Взаимодействие:**
- **Синхронное** — REST через Gateway (JWT-авторизация)
- **Асинхронное** — Apache Kafka (Outbox-паттерн → at-least-once)
- **Межсервисное** — OAuth2 Client Credentials Flow

---

## Безопасность

- **OAuth 2.0 / OpenID Connect** через Keycloak
- Пользователь входит через **Authorization Code Flow** → JWT access token
- Сервисы между собой — **Client Credentials Flow**
- Каждый сервис одновременно OAuth2 **Client** и **Resource Server**
- Актуатор-эндпоинты `/actuator/prometheus` открыты через `EndpointRequest.toAnyEndpoint().permitAll()`

---

## Тестовые пользователи

| Пользователь | Логин | Пароль |
|--------------|-------|--------|
| Клиент 1 | Alice | alice |
| Клиент 2 | Bob | bob |

---

## Структура проекта

```
my-bank-app/
├── accounts-service/       # Микросервис счетов
├── cash-service/           # Микросервис кассовых операций
├── transfer-service/       # Микросервис переводов
├── notifications-service/  # Микросервис уведомлений (Kafka consumer)
├── gateway-service/        # API Gateway
├── front-ui/               # Веб-интерфейс (Thymeleaf)
├── keycloak/               # Realm export
│
├── k8s/
│   ├── mybank/             # Helm-чарт приложения     → k8s/mybank/README.md
│   └── monitoring/         # Helm-чарт observability  → k8s/monitoring/README.md
│
├── jenkins/                # CI/CD (Jenkinsfile + Dockerfile)
│
├── docker-bake.hcl         # Multi-target Docker build
├── Dockerfile.build        # Multi-stage Dockerfile (локальная сборка)
├── Dockerfile.ci           # Lightweight Dockerfile (Jenkins CI, копирует JAR)
├── pom.xml                 # Корневой Maven POM
├── TECH_DEBT.md            # Технический долг
└── README.md               # Этот файл
```

---

## Варианты деплоя

### Локально (Kubernetes + Helm)

Требования: Docker Desktop с включённым Kubernetes, Helm v4+, kubectl.

```bash
# 1. Сборка образов
docker buildx bake --load -f docker-bake.hcl

# 2. Мониторинг (namespace: monitoring)
cd k8s/monitoring
helm dependency update
helm install k8s-monitoring . -n monitoring --create-namespace -f values-local.yaml

# 3. Приложение (namespace: mybank)
cd k8s/mybank
helm upgrade --install mybank . -n mybank --create-namespace -f values-local.yaml
```

Подробно: [k8s/mybank/README.md](k8s/mybank/README.md) и [k8s/monitoring/README.md](k8s/monitoring/README.md)

### Jenkins CI/CD

Полная автоматизация: тесты → сборка → push в GHCR → деплой в TEST → ручное подтверждение → PROD.

Подробно: [jenkins/README.md](jenkins/README.md)

---

## Observability

После деплоя `k8s/monitoring` доступны:

| Инструмент | Адрес | Назначение |
|------------|-------|-----------|
| Prometheus | localhost:9090 | Метрики, алерты |
| Grafana | localhost:3000 | Дашборды (admin/admin) |
| Kibana | localhost:5601 | Поиск и анализ логов |
| Zipkin | localhost:9411 | Трассировка запросов |
| Alertmanager | localhost:9093 | Маршрутизация алертов |

Логи всех сервисов пишутся в формате **Elastic Common Schema (ECS)**, автоматически собираются Filebeat и индексируются в Elasticsearch.

---

## Технологический стек

| Категория | Технологии |
|-----------|-----------|
| **Бэкенд** | Spring Boot 4.0, Spring Framework 7, Spring Cloud Gateway |
| **Безопасность** | Keycloak, OAuth2, JWT, Spring Security |
| **База данных** | PostgreSQL, Liquibase, JPA/Hibernate |
| **Очереди** | Apache Kafka 4.2 (KRaft, без ZooKeeper) |
| **Контейнеризация** | Docker, BuildKit, multi-stage build |
| **Оркестрация** | Kubernetes, Helm, Ingress-nginx |
| **CI/CD** | Jenkins, GHCR (GitHub Container Registry) |
| **Тестирование** | JUnit 5, Mockito, Testcontainers, Spring Cloud Contract, EmbeddedKafka |
| **Надёжность** | Resilience4j (Circuit Breaker, Retry), Outbox-паттерн |
| **Метрики** | Prometheus, Grafana, Micrometer, kube-prometheus-stack |
| **Логирование** | ELK Stack (Elasticsearch 8.12, Logstash, Kibana, Filebeat) |
| **Трейсинг** | Zipkin |

---

## Тесты

Проект покрыт тестами на нескольких уровнях. Безопасность **не отключается** — тесты эмулируют JWT.

| Сервис | Unit | Integration | Kafka IT | Contract (provider) | Contract (consumer) | Итого |
|--------|------|-------------|----------|---------------------|---------------------|-------|
| Cash Service | 3+ | 4 | 1 | 3 | — | 11+ |
| Transfer Service | 3+ | 4 | 1 | — | — | 8+ |
| Accounts Service | 3+ | — | 1 | 3 | — | 7+ |
| Notifications Service | 7 | 2 | — | — | — | 9 |
| Front UI | 5 | 8 | — | — | 3 | 16 |
| **Итого** | **21+** | **18+** | **3** | **6** | **3** | **51+** |

---

## Личный контекст

В этой работе я сознательно вышел из зоны комфорта — предыдущие проекты были на Gradle и WebFlux, здесь использованы **Maven** и классический **Spring MVC**. Это позволило глубже понять традиционный servlet-стек.

---

## Автор

Иван Васильев

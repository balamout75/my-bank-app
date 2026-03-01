# ☸ MyBank — Развёртывание в Kubernetes

[← Назад к основному README](../README.md)

---

## Обзор

В Kubernetes проект работает **без Eureka и Config Server**. Их функции выполняет сам Kubernetes:

| Docker Compose | Kubernetes |
|---------------|------------|
| Eureka (Service Discovery) | K8s DNS + Spring Cloud LoadBalancer (static instances) |
| Config Server | ConfigMap (application-k8s.yml) |
| nginx + ngrok | Ingress-nginx |
| docker-compose.env | Kubernetes Secrets |
| Spring профиль `docker` | Spring профиль `k8s` |

---

## Архитектура в кластере

```
┌──────────────────────────────────────────────────────────────┐
│  Kubernetes Cluster (namespace: mybank)                      │
│                                                              │
│  Browser → Ingress-nginx                                     │
│               ├── mybank.dev.local       → front-ui:8081     │
│               └── keycloak.mybank.dev.local → keycloak:8080  │
│                                                              │
│  front-ui → gateway-service:8090                             │
│                ├── accounts-service:8080                      │
│                ├── cash-service:8080                          │
│                ├── transfer-service:8080                      │
│                └── notifications-service:8080                 │
│                                                              │
│  Kafka (StatefulSet, KRaft) ← cash, transfer, accounts      │
│       └── notifications-service (consumer)                   │
│                                                              │
│  PostgreSQL (StatefulSet, 5 схем)                            │
│  Keycloak (Deployment, realm import)                         │
│                                                              │
│  ConfigMap — конфигурация каждого сервиса                    │
│  Secret — DB credentials, OAuth2 secrets, Kafka bootstrap    │
└──────────────────────────────────────────────────────────────┘
```

---

## Helm-чарты

```
k8s/
├── Chart.yaml                  # Зонтичный чарт (umbrella)
├── values.yaml                 # Глобальные настройки
├── values-local.yaml           # Локальные секреты (НЕ коммитить!)
├── templates/
│   ├── NOTES.txt               # Инструкции после деплоя
│   └── tests/                  # Helm-тесты
│       ├── test-health.yaml    # Проверка доступности всех сервисов
│       └── test-connectivity.yaml # Проверка маршрутов Gateway
└── charts/
    ├── postgresql/             # БД (StatefulSet)
    ├── kafka/                  # Kafka KRaft (StatefulSet)
    ├── keycloak/               # OAuth2 сервер (Deployment + realm import)
    ├── gateway-service/        # API Gateway (порт 8090)
    ├── front-ui/               # Веб-интерфейс (порт 8081, Ingress)
    ├── accounts-service/       # Счета (порт 8080)
    ├── cash-service/           # Наличные (порт 8080)
    ├── transfer-service/       # Переводы (порт 8080)
    └── notifications-service/  # Уведомления (порт 8080)
```

Каждый подчарт содержит:
- `templates/deployment.yaml` — Pod с контейнером сервиса
- `templates/service.yaml` — K8s Service (ClusterIP)
- `templates/configmap.yaml` — Spring Boot конфигурация (application-k8s.yml)
- `templates/secret.yaml` — DB credentials, OAuth2 client secret, Kafka bootstrap
- `values.yaml` — параметры по умолчанию (секреты — пустые placeholder'ы)

---

## Предусловия

- Docker Desktop с включённым Kubernetes
- Helm v4+
- kubectl

---

## Пошаговое развёртывание

### Шаг 1. Ingress-nginx

```bash
kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/controller-v1.12.0/deploy/static/provider/cloud/deploy.yaml
kubectl get pods -n ingress-nginx -w    # ждать 1/1 Running
```

### Шаг 2. DNS — Windows hosts

Файл: `C:\Windows\System32\drivers\etc\hosts` (от имени администратора)

```
127.0.0.1 mybank.local keycloak.mybank.local mybank.dev.local keycloak.mybank.dev.local mybank.test.local keycloak.mybank.test.local mybank.prod.local keycloak.mybank.prod.local
```
> 💡 **Почему указаны все окружения?** Настройки hosts и CoreDNS включают хосты для dev, test и prod, хотя для ручного деплоя достаточно только dev. Это сделано намеренно — чтобы DNS-конфигурация была **единой точкой настройки** для всех вариантов развёртывания (ручной K8s, Jenkins CI/CD). Настроив DNS один раз, вы сможете использовать любой вариант без дополнительных изменений.

### Шаг 3. CoreDNS rewrite

Сервисы внутри кластера обращаются к Keycloak по внешнему имени (для совпадения OAuth2 issuer в JWT-токене). CoreDNS перенаправляет это имя на внутренний сервис.

```bash
kubectl edit configmap coredns -n kube-system
```

Добавить **перед** строкой `kubernetes cluster.local`:

```
    rewrite name keycloak.mybank.local mybank-keycloak.mybank.svc.cluster.local
    rewrite name keycloak.mybank.dev.local mybank-keycloak.mybank.svc.cluster.local
    rewrite name keycloak.mybank.test.local mybank-test-keycloak.test.svc.cluster.local
    rewrite name keycloak.mybank.prod.local mybank-prod-keycloak.prod.svc.cluster.local
```

Перезапуск:

```bash
kubectl rollout restart deployment coredns -n kube-system
```

#### Зачем это нужно?

Браузер получает JWT-токен от `http://keycloak.mybank.dev.local/realms/mybank`. Токен содержит `iss: http://keycloak.mybank.dev.local/realms/mybank`. Сервисы проверяют issuer — он должен совпадать. Без CoreDNS rewrite сервисы не смогут обратиться к Keycloak по этому адресу.

### Шаг 4. Сборка образов

```bash
docker buildx bake --load -f docker-bake.hcl
```

### Шаг 5. Подготовка секретов

Создайте файл `k8s/values-local.yaml` (добавьте в `.gitignore`!):

```yaml
global:
  postgresql:
    password: "mybank_password"

accounts-service:
  keycloak:
    clientSecret: "your-accounts-client-secret"

cash-service:
  keycloak:
    clientSecret: "your-cash-client-secret"

transfer-service:
  keycloak:
    clientSecret: "your-transfer-client-secret"

front-ui:
  keycloak:
    clientSecret: "your-frontend-client-secret"

keycloak:
  admin:
    password: "admin"
```

> ⚠️ Этот файл содержит реальные секреты. Убедитесь что он в `.gitignore`:
> ```
> k8s/values-local.yaml
> ```

### Шаг 6. Деплой

```bash
cd k8s
helm dependency update .
helm install mybank . --namespace mybank --create-namespace -f values-local.yaml
```

В случае повторного деплоя:

```bash
helm upgrade --install mybank . --namespace mybank -f values-local.yaml
```

### Шаг 7. Проверка

```bash
kubectl get pods -n mybank -w    # ждать все 1/1 Running (2-3 минуты)
```

### Шаг 8. Helm Tests

После того как все поды готовы, запустите тесты:

```bash
helm test mybank -n mybank
```

Ожидаемый результат:

```
TEST SUITE:     mybank-test-connectivity
Phase:          Succeeded
TEST SUITE:     mybank-test-health
Phase:          Succeeded
```

### Шаг 9. Доступ

| Сервис | URL |
|--------|-----|
| Приложение | http://mybank.dev.local |
| Keycloak Admin | http://keycloak.mybank.dev.local/admin/ |

---

## Helm Tests

Тесты находятся в `k8s/templates/tests/` и запускаются как отдельные поды после деплоя.

### test-health

Проверяет доступность всех компонентов:

| Компонент | Проверка |
|-----------|---------|
| PostgreSQL | TCP порт 5432 |
| Keycloak | TCP порт 80 (Service) |
| Kafka | TCP порт 9092 |
| Front UI | TCP порт 8081 |
| Gateway Service | HTTP /actuator/health |
| Accounts Service | HTTP /actuator/health |
| Cash Service | HTTP /actuator/health |
| Transfer Service | HTTP /actuator/health |
| Notifications Service | HTTP /actuator/health |

### test-connectivity

Проверяет маршруты Gateway → backend-сервисы через HTTP-запросы к `/api/*` эндпоинтам.

### Повторный запуск тестов

При повторном запуске нужно удалить старые test-поды:

```bash
kubectl delete pod mybank-test-health -n mybank --ignore-not-found
kubectl delete pod mybank-test-connectivity -n mybank --ignore-not-found
helm test mybank -n mybank
```

### Диагностика при ошибке

```bash
# Какие контейнеры упали
kubectl describe pod mybank-test-health -n mybank | findstr "Name: Exit"

# Логи конкретного контейнера
kubectl logs mybank-test-health -n mybank -c test-keycloak
kubectl logs mybank-test-health -n mybank -c test-accounts
```

---

## Управление секретами

### Как секреты попадают в Pod

```
values-local.yaml (НЕ коммитится)
    │ helm install -f values-local.yaml
    ▼
values.yaml (пустые placeholder'ы "")
    │ перезаписываются через -f
    ▼
templates/secret.yaml → K8s Secret (Opaque)
    │
    ▼
deployment.yaml → envFrom: secretRef → Pod env vars
    │
    ▼
configmap.yaml → ${DB_URL}, ${KAFKA_BOOTSTRAP_SERVERS} (Spring Boot резолвит)
```

### Что хранится в K8s Secrets

| Сервис | Ключи в Secret |
|--------|---------------|
| accounts-service | DB_URL, DB_USER, DB_PASSWORD, OAUTH2_CLIENT_SECRET, KAFKA_BOOTSTRAP_SERVERS |
| cash-service | DB_URL, DB_USER, DB_PASSWORD, OAUTH2_CLIENT_SECRET, KAFKA_BOOTSTRAP_SERVERS |
| transfer-service | DB_URL, DB_USER, DB_PASSWORD, OAUTH2_CLIENT_SECRET, KAFKA_BOOTSTRAP_SERVERS |
| notifications-service | DB_URL, DB_USER, DB_PASSWORD, KAFKA_BOOTSTRAP_SERVERS |
| front-ui | OAUTH2_CLIENT_SECRET |
| keycloak | KEYCLOAK_ADMIN_PASSWORD |
| postgresql | POSTGRES_DB, POSTGRES_USER, POSTGRES_PASSWORD |

> ⚠️ Notifications-service не использует Keycloak (нет OAUTH2_CLIENT_SECRET). Front-ui не использует БД напрямую (секреты БД отсутствуют).

---

## Управление

### Обновление после изменения кода

```bash
docker buildx bake --load -f docker-bake.hcl
kubectl rollout restart deployment -n mybank
```

### Обновление после изменения Helm-чартов

```bash
cd k8s
helm upgrade --install mybank . --namespace mybank -f values-local.yaml
```

### Масштабирование

```bash
kubectl scale deployment mybank-notifications-service -n mybank --replicas=3
```

K8s Service автоматически балансирует нагрузку по round-robin.

### Логи

```bash
# Последние 50 строк
kubectl logs -n mybank -l app=accounts-service --tail=50

# Следить за логами в реальном времени
kubectl logs -n mybank -l app=accounts-service -f
```

### Отладка

```bash
# События и ошибки пода
kubectl describe pod -n mybank <pod-name>

# Shell внутри пода
kubectl exec -it -n mybank <pod-name> -- /bin/sh

# Проверка DNS
kubectl exec -n mybank <pod-name> -- nslookup keycloak.mybank.dev.local

# Проверка Keycloak
kubectl exec -n mybank <pod-name> -- curl -s http://keycloak.mybank.dev.local/realms/mybank

# Проверка секрета (PowerShell)
kubectl get secret mybank-accounts-service -n mybank -o jsonpath="{.data.KAFKA_BOOTSTRAP_SERVERS}"
```

### Удаление

```bash
helm uninstall mybank -n mybank
kubectl delete namespace mybank
```

### Полный сброс Kubernetes

Docker Desktop → Settings → Kubernetes → **Reset Kubernetes Cluster**

---

## Конфигурация

### values.yaml — глобальные параметры

```yaml
global:
  appHost: mybank.dev.local                     # хост приложения (Ingress)
  keycloakHost: keycloak.mybank.dev.local       # хост Keycloak (Ingress + issuer)
  postgresql:
    port: 5432
    database: mybank
    username: mybank
    password: ""    # передаётся через values-local.yaml или --set
```

### values-local.yaml — локальные секреты

```yaml
global:
  postgresql:
    password: "mybank_password"

accounts-service:
  keycloak:
    clientSecret: "..."

# ... остальные сервисы
```

### Как параметры попадают в Pod

```
values.yaml + values-local.yaml
    │
    ├──→ secret.yaml     → env vars (DB_URL, DB_PASSWORD, OAUTH2_CLIENT_SECRET, KAFKA_BOOTSTRAP_SERVERS)
    ├──→ configmap.yaml  → application-k8s.yml (монтируется как файл)
    └──→ deployment.yaml → image, ports, probes, volumes
```

Deployment монтирует ConfigMap как файл `/config/application-k8s.yml` и загружает Secret как environment variables. Spring Boot читает `${DB_URL}`, `${DB_PASSWORD}`, `${KAFKA_BOOTSTRAP_SERVERS}` из окружения.

### Переопределение параметров

```bash
helm install mybank . -n mybank --create-namespace \
  -f values-local.yaml \
  --set global.appHost=mybank.prod.local \
  --set accounts-service.image.tag=42
```

---

## Отличия от Docker Compose

| Аспект | Docker Compose | Kubernetes |
|--------|---------------|------------|
| Service Discovery | Eureka | K8s DNS + static instances в ConfigMap |
| Конфигурация | Config Server (Git) | ConfigMap (application-k8s.yml) |
| Прокси | nginx container | Ingress-nginx |
| DNS | Docker DNS | CoreDNS + rewrite |
| Секреты | .env файл | K8s Secrets (через Helm + values-local.yaml) |
| Spring профиль | `docker` | `k8s` |
| Config import | `configserver:http://...` | `file:/config/application-k8s.yml` |
| Балансировка | Eureka + Ribbon | K8s Service (round-robin) |
| Порядок запуска | `depends_on` + healthcheck | Probes + restart policy |
| Тесты | — | Helm Tests (health + connectivity) |
| Kafka | docker-compose service | K8s StatefulSet (KRaft) |

---

## Частые проблемы

| Проблема | Причина | Решение |
|----------|---------|---------|
| `CrashLoopBackOff` | Keycloak недоступен | Проверить CoreDNS rewrite |
| `No servers available` | Нет static instances | Добавить в ConfigMap `spring.cloud.discovery.client.simple.instances` |
| `403 Forbidden` | Неправильные роли | Проверить client-id и роли в Keycloak |
| `Invalid redirect_uri` | Хост не в redirectUris | Добавить хост в `realm-mybank.json` |
| `UnknownHostException` | Хардкод DB host | Должен быть `{{ .Release.Name }}-postgresql` |
| `context deadline exceeded` | Поды не стартуют за timeout | Проверить логи `kubectl logs`, увеличить `--timeout` |
| Образ не обновился | Кэш Docker/K8s | `docker buildx bake --load` + `kubectl rollout restart` |
| `helm test` Failed | Старые test-поды | `kubectl delete pod <test-pod> --ignore-not-found` |
| `localhost:9092` в логах | Нет KAFKA_BOOTSTRAP_SERVERS | Добавить в secret.yaml и configmap (`${KAFKA_BOOTSTRAP_SERVERS}`) |
| `no main manifest attribute` | Нет spring-boot-maven-plugin | Добавить плагин в pom.xml сервиса |
| Секреты в Git | Plaintext в values.yaml | Использовать `values-local.yaml` (в `.gitignore`) |

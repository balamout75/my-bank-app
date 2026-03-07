# ☸ MyBank — Развёртывание приложения в Kubernetes

[← Назад к README](../../README.md)

---

## Обзор

Helm-чарт `k8s/mybank` разворачивает всё приложение MyBank в namespace `mybank`.
В Kubernetes проект работает **без Eureka и Config Server** — их функции берёт на себя сам кластер.

| Docker Compose | Kubernetes |
|----------------|-----------|
| Eureka (Service Discovery) | K8s DNS + Spring Cloud LoadBalancer |
| Config Server | ConfigMap (`application-k8s.yml`) |
| nginx + ngrok | Ingress-nginx |
| `.env` файл | Kubernetes Secrets |
| Spring профиль `docker` | Spring профиль `k8s` |

---

## Архитектура в кластере

```
┌──────────────────────────────────────────────────────────────────┐
│  namespace: mybank                                               │
│                                                                  │
│  Browser → Ingress-nginx                                         │
│               ├── mybank.dev.local         → front-ui:8081       │
│               └── keycloak.mybank.dev.local → keycloak:8080      │
│                                                                  │
│  front-ui → gateway-service:8090                                 │
│                ├── accounts-service:8080                         │
│                ├── cash-service:8080                             │
│                ├── transfer-service:8080                         │
│                └── notifications-service:8080                    │
│                                                                  │
│  Kafka (StatefulSet, KRaft) ← cash, transfer, accounts           │
│       └── notifications-service (consumer)                       │
│                                                                  │
│  PostgreSQL (StatefulSet, 5 схем)                                │
│  Keycloak   (Deployment, auto realm import)                      │
│  Zipkin     (Deployment, ES storage → namespace monitoring)      │
│                                                                  │
│  ConfigMap — application-k8s.yml для каждого сервиса            │
│  Secret    — DB credentials, OAuth2 secrets, Kafka bootstrap     │
└──────────────────────────────────────────────────────────────────┘
```

---

## Структура чарта

```
k8s/mybank/
├── Chart.yaml
├── values.yaml
├── values-local.yaml             # Локальные секреты (не коммитить!)
├── templates/
│   ├── NOTES.txt
│   ├── prometheusrule.yaml
│   └── tests/
│       ├── test-health.yaml
│       └── test-connectivity.yaml
└── charts/
    ├── postgresql/
    ├── kafka/
    ├── keycloak/
    ├── gateway-service/
    ├── front-ui/
    ├── accounts-service/         # + ServiceMonitor
    ├── cash-service/             # + ServiceMonitor
    ├── transfer-service/         # + ServiceMonitor
    ├── notifications-service/    # + ServiceMonitor
    └── zipkin/
```

---

## Предусловия

- Docker Desktop с включённым Kubernetes
- Helm v4+
- kubectl
- Ingress-nginx установлен в кластере

---

## Пошаговое развёртывание

> ⚠️ Все команды выполняются из корня проекта (`my-bank-app/`), если не указано иное.

### Шаг 1. Ingress-nginx

```bash
# из любой директории
kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/controller-v1.12.0/deploy/static/provider/cloud/deploy.yaml

# ждать 1/1 Running
kubectl get pods -n ingress-nginx -w
```

### Шаг 2. DNS — /etc/hosts

**Windows** — `C:\Windows\System32\drivers\etc\hosts` (от имени администратора)
**Linux / macOS** — `/etc/hosts`

```
# Приложение
127.0.0.1  mybank.dev.local  keycloak.mybank.dev.local

# Мониторинг
127.0.0.1  prometheus.monitoring.local  grafana.monitoring.local
127.0.0.1  alertmanager.monitoring.local  kibana.monitoring.local  zipkin.monitoring.local
```

> 💡 Если планируете Jenkins CI/CD — добавьте сразу все среды: `mybank.test.local keycloak.mybank.test.local mybank.prod.local keycloak.mybank.prod.local`.

### Шаг 3. CoreDNS rewrite

Сервисы внутри кластера обращаются к Keycloak по **внешнему** имени — чтобы `iss` в JWT-токене совпадал при валидации.

```bash
kubectl edit configmap coredns -n kube-system
```

Добавить **перед** строкой `kubernetes cluster.local`:

```
    rewrite name keycloak.mybank.dev.local mybank-keycloak.mybank.svc.cluster.local
    rewrite name keycloak.mybank.test.local mybank-test-keycloak.test.svc.cluster.local
    rewrite name keycloak.mybank.prod.local mybank-prod-keycloak.prod.svc.cluster.local
```

> ⚠️ Команда чувствительна к форматированию и отступам. Проверьте что правила добавились корректно.

```bash
kubectl rollout restart deployment coredns -n kube-system
```

### Шаг 4. Сборка Docker-образов

> 📁 Выполнять из **корня проекта** — там где лежит `docker-bake.hcl`:
> ```
> my-bank-app/        ← отсюда
>   docker-bake.hcl
>   k8s/
>   ...
> ```

```bash
docker buildx bake --load -f docker-bake.hcl
```

Maven на хосте **не нужен** — всё собирается внутри Docker (multi-stage build).

> ⚠️ Команда может не выполниться с первого раза из-за параллельной сборки образов. Если упала с ошибкой — просто запустите повторно, со второго раза отрабатывает стабильно.

### Шаг 5. Подготовка секретов

Создайте файл `k8s/mybank/values-local.yaml` (добавьте в `.gitignore`!):

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

> Client secrets берутся из Keycloak Admin → Clients → `<client-name>` → Credentials.

### Шаг 6. Деплой мониторинга

ServiceMonitor CRD должны существовать в кластере **до** деплоя mybank.

> 📁 Переходим в папку мониторинга:

```bash
cd k8s/monitoring
helm dependency update
helm install k8s-monitoring . -n monitoring --create-namespace -f values-local.yaml
```

Следим за поднятием подов:

```bash
kubectl get pods -n monitoring -w
```

Ожидаемый порядок готовности: Elasticsearch → Prometheus/Grafana → Logstash → Filebeat → Kibana.

> ⚠️ **Kibana стартует 2–4 минуты.** На старте вы увидите `0/1` и возможно кратковременный `CrashLoopBackOff` — это нормально, Kibana ждёт пока Elasticsearch примет соединения. Дождитесь `1/1 Running` прежде чем идти дальше.

Когда все поды `1/1 Running` — проверьте через браузер:

| Сервис | URL | Credentials |
|--------|-----|-------------|
| Prometheus | http://prometheus.monitoring.local | — |
| Grafana | http://grafana.monitoring.local | admin / admin |
| Alertmanager | http://alertmanager.monitoring.local | — |
| Kibana | http://kibana.monitoring.local | — |
| Zipkin | http://zipkin.monitoring.local | — |

### Шаг 7. Деплой приложения

> 📁 Переходим в папку приложения:

**Первая установка** (namespace не существует):
```bash
cd k8s/mybank
helm install mybank . -n mybank --create-namespace -f values-local.yaml
```

**Обновление** (namespace уже существует):
```bash
cd k8s/mybank
helm upgrade mybank . -n mybank -f values-local.yaml
```

> 💡 `helm upgrade --install` работает в обоих случаях, но явное разделение помогает понять что происходит — особенно если деплой упал и нужно разобраться на каком шаге.

### Шаг 8. Проверка готовности

> 📁 Из любой директории:

```bash
kubectl get pods -n mybank -w
```

> ⚠️ **Порядок запуска важен.** Большинство сервисов зависят от Keycloak — они будут в `CrashLoopBackOff` пока он не поднимется. Это нормально. Сначала стартуют PostgreSQL и Kafka, затем Keycloak (~3–5 минут), и только после этого остальные сервисы.

Ожидаемый итог — все поды `1/1 Running`:

```
mybank-postgresql-0                1/1 Running   ← стартует первым
mybank-kafka-0                     1/1 Running   ← стартует первым
mybank-keycloak-xxxx               1/1 Running   ← ключевой момент, ~3-5 мин
mybank-accounts-service-xxxx       1/1 Running   ← после Keycloak
mybank-cash-service-xxxx           1/1 Running
mybank-transfer-service-xxxx       1/1 Running
mybank-notifications-service-xxxx  1/1 Running
mybank-gateway-service-xxxx        1/1 Running
mybank-front-ui-xxxx               1/1 Running
mybank-zipkin-xxxx                 1/1 Running
```

Общее время первого запуска: **5–10 минут**.

### Шаг 9. Helm Tests

```bash
helm test mybank -n mybank
```

Ожидаемый результат:

```
TEST SUITE: mybank-test-health        Phase: Succeeded
TEST SUITE: mybank-test-connectivity  Phase: Succeeded
```

### Шаг 10. Доступ к приложению

| Сервис | URL | Credentials |
|--------|-----|-------------|
| Приложение | http://mybank.dev.local | alice/alice, bob/bob |
| Keycloak Admin | http://keycloak.mybank.dev.local/admin/ | admin/admin |

---

## Helm Tests

### test-health

| Компонент | Проверка |
|-----------|----------|
| PostgreSQL | TCP :5432 |
| Keycloak | TCP :80 |
| Kafka | TCP :9092 |
| Front UI | TCP :8081 |
| Gateway Service | HTTP /actuator/health |
| Accounts Service | HTTP /actuator/health |
| Cash Service | HTTP /actuator/health |
| Transfer Service | HTTP /actuator/health |
| Notifications Service | HTTP /actuator/health |

### test-connectivity

Проверяет маршруты Gateway → backend-сервисы через HTTP-запросы к `/api/*`.

### Повторный запуск тестов

```bash
kubectl delete pod mybank-test-health mybank-test-connectivity -n mybank --ignore-not-found
helm test mybank -n mybank
```

---

## Управление секретами

```
values-local.yaml  (не коммитится)
        │ helm install -f values-local.yaml
        ▼
values.yaml (пустые placeholder'ы)
        │
        ▼
secret.yaml → K8s Secret (Opaque, base64)
        │
        ▼
deployment.yaml → envFrom: secretRef → env vars в Pod
        │
        ▼
configmap.yaml → ${DB_URL}, ${KAFKA_BOOTSTRAP_SERVERS}
```

| Сервис | Ключи |
|--------|-------|
| accounts-service | DB_URL, DB_USER, DB_PASSWORD, OAUTH2_CLIENT_SECRET, KAFKA_BOOTSTRAP_SERVERS |
| cash-service | DB_URL, DB_USER, DB_PASSWORD, OAUTH2_CLIENT_SECRET, KAFKA_BOOTSTRAP_SERVERS |
| transfer-service | DB_URL, DB_USER, DB_PASSWORD, OAUTH2_CLIENT_SECRET, KAFKA_BOOTSTRAP_SERVERS |
| notifications-service | DB_URL, DB_USER, DB_PASSWORD, KAFKA_BOOTSTRAP_SERVERS |
| front-ui | OAUTH2_CLIENT_SECRET |
| keycloak | KEYCLOAK_ADMIN_PASSWORD |
| postgresql | POSTGRES_DB, POSTGRES_USER, POSTGRES_PASSWORD |

---

## Управление чартом

### Обновление образов после изменения кода

> 📁 Из **корня проекта**:

```bash
docker buildx bake --load -f docker-bake.hcl
kubectl rollout restart deployment -n mybank
```

### Обновление Helm-чарта

> 📁 Из `k8s/mybank/`:

```bash
helm upgrade --install mybank . -n mybank -f values-local.yaml
```

### Масштабирование

```bash
kubectl scale deployment mybank-notifications-service -n mybank --replicas=3
```

### Удаление

```bash
helm uninstall mybank -n mybank
kubectl delete namespace mybank
```

---

## Диагностика

```bash
# Логи сервиса (реальное время)
kubectl logs -n mybank -l app=accounts-service --tail=50 -f

# События пода
kubectl describe pod -n mybank <pod-name>

# Shell внутри контейнера
kubectl exec -it -n mybank <pod-name> -- /bin/sh

# Проверить CoreDNS rewrite
kubectl exec -n mybank <pod-name> -- nslookup keycloak.mybank.dev.local

# Проверить секрет (PowerShell)
kubectl get secret mybank-accounts-service -n mybank -o jsonpath="{.data.DB_URL}" | base64 -d
```

---

## Частые проблемы

| Проблема | Причина | Решение |
|----------|---------|---------| 
| `CrashLoopBackOff` у сервисов при старте | Keycloak ещё не готов | Подождать — сервисы поднимутся сами после Keycloak |
| `401 Unauthorized` на /actuator/prometheus | SecurityConfig блокирует Prometheus | `EndpointRequest.toAnyEndpoint().permitAll()` в SecurityConfig |
| `No servers available` | Нет static instances в ConfigMap | Проверить `spring.cloud.discovery.client.simple.instances` |
| `403 Forbidden` | Неправильные роли Keycloak | Проверить client-id и роли в Keycloak Admin |
| `Invalid redirect_uri` | Хост не в redirectUris | Добавить хост в `realm-mybank.json` |
| Образ не обновился | Кэш K8s | `kubectl rollout restart deployment -n mybank` |
| `helm test` Failed | Старые test-поды | `kubectl delete pod mybank-test-health mybank-test-connectivity -n mybank` |
| ServiceMonitor не работает | CRD отсутствуют | Сначала задеплоить `k8s/monitoring` (шаг 6) |
| `docker buildx bake` упал | Параллельная сборка | Запустить команду повторно |
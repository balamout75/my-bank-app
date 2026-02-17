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
│  PostgreSQL (StatefulSet, 5 схем)                            │
│  Keycloak (Deployment, realm import)                         │
│                                                              │
│  ConfigMap — конфигурация каждого сервиса                    │
│  Secret — DB credentials, OAuth2 client secrets              │
└──────────────────────────────────────────────────────────────┘
```

---

## Helm-чарты

```
k8s/
├── Chart.yaml                  # Зонтичный чарт (umbrella)
├── values.yaml                 # Глобальные настройки
├── templates/
│   └── NOTES.txt               # Инструкции после деплоя
└── charts/
    ├── postgresql/             # БД (StatefulSet)
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
- `templates/secret.yaml` — DB credentials, OAuth2 client secret
- `values.yaml` — параметры по умолчанию

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
127.0.0.1 mybank.dev.local keycloak.mybank.dev.local
```

### Шаг 3. CoreDNS rewrite

Сервисы внутри кластера обращаются к Keycloak по внешнему имени (для совпадения OAuth2 issuer в JWT-токене). CoreDNS перенаправляет это имя на внутренний сервис.

```bash
kubectl edit configmap coredns -n kube-system
```

Добавить **перед** строкой `kubernetes cluster.local`:

```
    rewrite name keycloak.mybank.dev.local mybank-keycloak.mybank.svc.cluster.local
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

### Шаг 5. Деплой

```bash
cd k8s
helm dependency update .
helm install mybank . --namespace mybank --create-namespace \
  --set global.postgresql.password=mybank_password
```

### Шаг 6. Проверка

```bash
kubectl get pods -n mybank -w    # ждать все 1/1 Running (2-3 минуты)
```

### Шаг 7. Доступ

| Сервис | URL |
|--------|-----|
| Приложение | http://mybank.dev.local |
| Keycloak Admin | http://keycloak.mybank.dev.local/admin/ |

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
helm upgrade mybank . --namespace mybank
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
    password: ""    # передаётся через --set
```

### Как параметры попадают в Pod

```
values.yaml
    │
    ├──→ secret.yaml     → env vars (DB_URL, DB_PASSWORD, OAUTH2_CLIENT_SECRET)
    ├──→ configmap.yaml  → application-k8s.yml (монтируется как файл)
    └──→ deployment.yaml → image, ports, probes, volumes
```

Deployment монтирует ConfigMap как файл `/config/application-k8s.yml` и загружает Secret как environment variables. Spring Boot читает `${DB_URL}`, `${DB_PASSWORD}` из окружения.

### Переопределение параметров

```bash
helm install mybank . -n mybank --create-namespace \
  --set global.appHost=mybank.prod.local \
  --set global.keycloakHost=keycloak.mybank.prod.local \
  --set global.postgresql.password=secure_password \
  --set accounts-service.keycloak.clientSecret=xxx \
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
| Секреты | .env файл | K8s Secrets (через Helm) |
| Spring профиль | `docker` | `k8s` |
| Config import | `configserver:http://...` | `file:/config/application-k8s.yml` |
| Балансировка | Eureka + Ribbon | K8s Service (round-robin) |
| Порядок запуска | `depends_on` + healthcheck | Probes + restart policy |

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

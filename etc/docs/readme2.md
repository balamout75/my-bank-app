# MyBank — Микросервисное банковское приложение

## Архитектура

```
┌─────────────┐     ┌──────────────┐     ┌───────────────────┐
│   Browser   │────▶│ Ingress/Nginx│────▶│    Front-UI        │
└─────────────┘     └──────────────┘     │   (Thymeleaf)      │
                                          └────────┬──────────┘
                                                   │
                                          ┌────────▼──────────┐
                                          │  Gateway Service   │
                                          └────────┬──────────┘
                           ┌───────────────────────┼───────────────────────┐
                           │                       │                       │
                  ┌────────▼──────┐    ┌──────────▼────────┐   ┌─────────▼─────────┐
                  │   Accounts    │    │      Cash          │   │    Transfer        │
                  │   Service     │    │    Service         │   │    Service         │
                  └───────┬───────┘    └──────┬──┬──────────┘   └──────┬──┬─────────┘
                          │                   │  │                     │  │
                          │            ┌──────┘  └──────┐       ┌──────┘  └──────┐
                          │            │                │       │                │
                          ▼            ▼                ▼       ▼                ▼
                  ┌──────────────┐  ┌──────────┐  ┌──────────────────┐  ┌──────────┐
                  │ Notifications│  │ Accounts │  │  Notifications   │  │ Accounts │
                  │   Service    │  │ Service  │  │    Service       │  │ Service  │
                  └──────────────┘  └──────────┘  └──────────────────┘  └──────────┘
                                          │
                                   ┌──────▼──────┐    ┌──────────────┐
                                   │ PostgreSQL  │    │   Keycloak   │
                                   │  (5 схем)   │    │   (OAuth2)   │
                                   └─────────────┘    └──────────────┘
```

## Микросервисы

| Сервис | Описание | Порт |
|--------|----------|------|
| front-ui | Веб-интерфейс (Thymeleaf) | 8081 |
| gateway-service | API Gateway (маршрутизация) | 8090 |
| accounts-service | Управление счетами | 8080 |
| cash-service | Операции с наличными | 8080 |
| transfer-service | Переводы между счетами | 8080 |
| notifications-service | Уведомления (outbox pattern) | 8080 |
| keycloak | OAuth2/OpenID Connect сервер | 8080 |
| postgresql | База данных (5 схем) | 5432 |

## Тестовые пользователи

| Логин | Пароль |
|-------|--------|
| alice | alice |
| bob | bob |

Keycloak admin: admin / admin

---

## Три варианта развёртывания

### Вариант 1: Docker Compose (разработка)

Использует Eureka, Config Server, nginx-прокси. Подходит для локальной разработки.

**Предусловия:** Docker, Docker Compose

**Запуск:**
```bash
# 1. Создайте .env из примера
cp docker-compose.env.example docker-compose.env
# Заполните секреты в docker-compose.env

# 2. Соберите образы
docker buildx bake --load -f docker-bake.hcl

# 3. Запустите
docker compose --env-file docker-compose.env up -d

# 4. Откройте
# http://localhost:8081
```

**Остановка:**
```bash
docker compose down
```

**Конфигурация:** `docker-compose.env`
```env
KC_CLIENT_SECRET_ACCOUNTS_SERVICE=...
KC_CLIENT_SECRET_CASH_SERVICE=...
KC_CLIENT_SECRET_TRANSFER_SERVICE=...
KC_CLIENT_SECRET_NOTIFICATIONS_SERVICE=...
```

---

### Вариант 2: Kubernetes с Helm (ручной деплой)

Без Eureka и Config Server. Использует K8s DNS, ConfigMap, Ingress-nginx. Подходит для тестирования K8s-деплоя.

**Предусловия:** Docker Desktop с Kubernetes, Helm, kubectl

**Шаг 1. Ingress-nginx:**
```bash
kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/controller-v1.12.0/deploy/static/provider/cloud/deploy.yaml
kubectl get pods -n ingress-nginx -w  # ждать 1/1 Running
```

**Шаг 2. DNS (Windows hosts):**
```
# C:\Windows\System32\drivers\etc\hosts
127.0.0.1 mybank.local keycloak.mybank.local
```

**Шаг 3. CoreDNS rewrite:**
```bash
kubectl edit configmap coredns -n kube-system
# Добавить перед "kubernetes cluster.local":
#   rewrite name keycloak.mybank.local mybank-keycloak.mybank.svc.cluster.local
kubectl rollout restart deployment coredns -n kube-system
```

**Шаг 4. Сборка образов:**
```bash
docker buildx bake --load -f docker-bake.hcl
```

**Шаг 5. Деплой:**
```bash
cd k8s
helm dependency update .
helm install mybank . --namespace mybank --create-namespace \
  --set global.postgresql.password=mybank_password
```

**Шаг 6. Проверка:**
```bash
kubectl get pods -n mybank -w  # ждать все 1/1 Running
```

**Доступ:**
- Приложение: http://mybank.local
- Keycloak: http://keycloak.mybank.local/admin/

**Обновление:**
```bash
docker buildx bake --load -f docker-bake.hcl
helm upgrade mybank . --namespace mybank
kubectl rollout restart deployment -n mybank
```

**Удаление:**
```bash
helm uninstall mybank -n mybank
```

---

### Вариант 3: Jenkins CI/CD (автоматизация)

Полный CI/CD pipeline: тесты → сборка → push в GHCR → деплой в TEST → approval → деплой в PROD.

**Предусловия:** Docker Desktop с Kubernetes, Helm, kubectl, GitHub аккаунт

**Шаг 1. GitHub:**
- Создайте репозиторий и запушьте проект
- Создайте Personal Access Token (classic): `repo`, `write:packages`, `read:packages`
- Создайте Fine-grained token для доступа к репозиторию: `contents: read-only`, `metadata: read-only`

**Шаг 2. Настройте jenkins/.env:**
```env
GITHUB_USERNAME=ваш-username
GITHUB_TOKEN=ghp_ваш-токен
GITHUB_REPOSITORY=ваш-username/my-bank-app
GHCR_TOKEN=ghp_ваш-токен
DOCKER_REGISTRY=ghcr.io/ваш-username
KUBECONFIG_PATH=C:/Users/ваше-имя/.kube/config
DB_PASSWORD=mybank_password
KC_SECRET_ACCOUNTS=Ycm7AwLxKchJ76kaRwDaG0RyHsp1T2rK
KC_SECRET_CASH=GJj0e0li8KrcaHb9S20ze8SDo8lO1zIL
KC_SECRET_TRANSFER=RnGnXR7iX8ZSgb5lSiiG53QteuGrKC9h
KC_SECRET_NOTIFICATIONS=EVSiTy4uHONBrbjFSt2BuWj2CWjV3vog
```

**Шаг 3. Подготовьте инфраструктуру K8s:**
```bash
# Ingress-nginx
kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/controller-v1.12.0/deploy/static/provider/cloud/deploy.yaml

# CoreDNS rewrite (для test и prod)
kubectl edit configmap coredns -n kube-system
# Добавить:
#   rewrite name keycloak.test.local mybank-test-keycloak.test.svc.cluster.local
#   rewrite name keycloak.prod.local mybank-prod-keycloak.prod.svc.cluster.local
kubectl rollout restart deployment coredns -n kube-system

# Windows hosts
# 127.0.0.1 mybank.test.local keycloak.test.local mybank.prod.local keycloak.prod.local
```

**Шаг 4. Запустите Jenkins:**
```bash
cd jenkins
docker compose up --build
```

**Шаг 5. Откройте Jenkins:**
- http://localhost:8080
- Проект MyBank создаётся автоматически
- Scan Repository Now → Build запустится

**Pipeline:**
```
mvn clean install → docker build → docker push (GHCR)
    → helm deploy (TEST) → ✅ Verify
    → ⏸ Manual Approval
    → helm deploy (PROD) → ✅ Verify
```

**Доступ после деплоя:**

| Окружение | Приложение | Keycloak |
|-----------|-----------|----------|
| TEST | http://mybank.test.local | http://keycloak.test.local/admin/ |
| PROD | http://mybank.prod.local | http://keycloak.prod.local/admin/ |

---

## Структура проекта

```
my-bank-app/
├── accounts-service/          # Микросервис счетов
├── cash-service/              # Микросервис наличных
├── transfer-service/          # Микросервис переводов
├── notifications-service/     # Микросервис уведомлений
├── gateway-service/           # API Gateway
├── front-ui/                  # Веб-интерфейс
├── config-service/            # Spring Cloud Config (Docker Compose)
├── discovery-service/         # Eureka (Docker Compose)
├── keycloak/                  # Realm export
├── proxy/                     # Nginx config (Docker Compose)
├── docker-compose.yml         # Вариант 1: Docker Compose
├── docker-compose.env         # Секреты для Docker Compose
├── docker-bake.hcl            # Multi-target Docker build
├── Dockerfile.build           # Multi-stage Dockerfile
├── k8s/                       # Вариант 2: Kubernetes Helm чарты
│   ├── Chart.yaml
│   ├── values.yaml
│   ├── templates/
│   │   ├── NOTES.txt
│   │   └── ingress.yaml
│   └── charts/
│       ├── postgresql/
│       ├── keycloak/
│       ├── gateway-service/
│       ├── front-ui/
│       ├── accounts-service/
│       ├── cash-service/
│       ├── transfer-service/
│       └── notifications-service/
└── jenkins/                   # Вариант 3: CI/CD
    ├── Dockerfile             # Jenkins с helm, kubectl, docker
    ├── docker-compose.yml     # Запуск Jenkins
    ├── .env                   # Секреты (не коммитить!)
    ├── .env.example           # Пример секретов
    ├── plugins.txt            # Плагины Jenkins
    ├── Jenkinsfile            # Pipeline
    └── init.groovy.d/
        ├── 01_secure-credentials.groovy
        └── 02_create-multibranch-job.groovy
```

## Отличия вариантов деплоя

| | Docker Compose | Kubernetes | Jenkins CI/CD |
|---|---|---|---|
| Service Discovery | Eureka | K8s DNS + Static LB | K8s DNS + Static LB |
| Конфигурация | Config Server | ConfigMap | ConfigMap |
| Прокси | nginx + ngrok | Ingress-nginx | Ingress-nginx |
| БД хост | `postgres` | `{{ .Release.Name }}-postgresql` | `{{ .Release.Name }}-postgresql` |
| Keycloak URL | ngrok domain | keycloakHost (CoreDNS) | keycloakHost (CoreDNS) |
| Секреты | `.env` файл | `values.yaml` / `--set` | Jenkins Credentials |
| Образы | Local build | Local build | GHCR (ghcr.io) |
| Spring профиль | `docker` | `k8s` | `k8s` |
| Окружения | Одно | Одно | TEST + PROD |

## Частые проблемы

| Проблема | Решение |
|----------|---------|
| CrashLoopBackOff | Проверить CoreDNS rewrite и логи: `kubectl logs -n mybank <pod>` |
| 403 Forbidden | Проверить роли в Keycloak и SecurityConfig |
| No servers available | Добавить static instances в ConfigMap |
| Invalid redirect_uri | Добавить хосты в redirectUris realm-mybank.json |
| Connection refused keycloak | CoreDNS rewrite не настроен |
| context deadline exceeded | Увеличить `--timeout` или проверить ресурсы |

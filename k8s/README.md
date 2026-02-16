# MyBank — Kubernetes Helm Charts

Зонтичный Helm-чарт для развёртывания микросервисного приложения «Банк» в Kubernetes.

## Архитектура в K8s

```
                          ┌─────────────────────────────────────────────┐
                          │                  Ingress                     │
                          │  mybank.local          keycloak.mybank.local │
                          └────┬──────┬───────────────────┬─────────────┘
                               │      │                   │
                        /      │      │ /api/*            │ /
                               │      │                   │
                    ┌──────────▼──┐ ┌─▼──────────────┐ ┌──▼──────────┐
                    │  front-ui   │ │ gateway-service │ │  keycloak   │
                    │  :8081      │ │ :8090           │ │  :8080      │
                    └──────┬──────┘ └───┬──┬──┬──┬───┘ └─────────────┘
                           │            │  │  │  │
                   gateway.url          │  │  │  │  K8s DNS (Service Discovery)
                     (K8s DNS)          │  │  │  │
                                        │  │  │  │
              ┌─────────────────────────┘  │  │  └─────────────────────┐
              │              ┌─────────────┘  └──────────┐             │
    ┌─────────▼──────┐ ┌────▼─────────┐ ┌───────────────▼┐ ┌─────────▼──────────┐
    │accounts-service│ │ cash-service │ │transfer-service│ │notifications-service│
    │  :8080         │ │ :8080        │ │ :8080          │ │ :8080               │
    └───────┬────────┘ └──────┬───────┘ └───────┬────────┘ └─────────┬───────────┘
            │                 │                 │                     │
            └─────────────────┴─────────┬───────┴─────────────────────┘
                                        │
                              ┌─────────▼──────────┐
                              │ PostgreSQL          │
                              │ (StatefulSet)       │
                              │ :5432               │
                              └─────────────────────┘
```

## Что заменяем из Spring Cloud → Kubernetes

| Spring Cloud             | Kubernetes                      | Реализация                                |
|--------------------------|--------------------------------|-------------------------------------------|
| Eureka (Service Discovery) | K8s DNS (Service)            | `http://mybank-accounts-service:8080`     |
| Spring Cloud Config      | ConfigMap + Secret              | `application-k8s.yml` в ConfigMap          |
| Config Server (Git)      | Helm values                     | `values.yaml` → шаблоны ConfigMap          |
| Docker Compose           | Helm Chart                      | Зонтичный чарт + сабчарты                |
| Nginx (reverse proxy)    | Ingress                         | nginx-ingress controller                  |

## Предварительные требования

1. **Kubernetes кластер** — Docker Desktop с включённым K8s
2. **Helm 3** — `brew install helm` (macOS) или [официальная установка](https://helm.sh/docs/intro/install/)
3. **Ingress Controller** — nginx-ingress:

```bash
kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/controller-v1.12.0/deploy/static/provider/cloud/deploy.yaml
```

4. **Записи в /etc/hosts**:
```
127.0.0.1  mybank.local  keycloak.mybank.local
```

## Шаг 1. Сборка Docker-образов

Из корня проекта (рядом с docker-compose.yaml):

```bash
# Сборка JAR (если ещё не собрано)
cd gateway-service && ./mvnw clean package -DskipTests && cd ..
cd front-ui && ./mvnw clean package -DskipTests && cd ..
cd accounts-service && ./mvnw clean package -DskipTests && cd ..
# ... аналогично для cash-service, transfer-service, notifications-service

# Сборка Docker-образов (для Docker Desktop K8s образы сразу доступны)
docker build -t mybank/yp-mybank-gateway:latest ./gateway-service
docker build -t mybank/yp-mybank-frontend:latest ./front-ui
docker build -t mybank/yp-mybank-accounts:latest ./accounts-service
# ... аналогично для остальных сервисов
```

## Шаг 2. Развёртывание

### Всё сразу (зонтичный чарт):
```bash
cd mybank-chart

# Обновить зависимости (связать сабчарты)
helm dependency update .

# Установить
helm install mybank . --namespace mybank --create-namespace

# Проверить статус
kubectl get pods -n mybank -w
```

### Отдельный сабчарт (например, только PostgreSQL):
```bash
helm install mybank-pg ./charts/postgresql \
  --namespace mybank --create-namespace \
  --set global.postgresql.host=mybank-pg-postgresql \
  --set global.postgresql.database=mybank \
  --set global.postgresql.username=mybank \
  --set global.postgresql.password=mybank_password
```

## Шаг 3. Проверка

```bash
# Все поды запущены?
kubectl get pods -n mybank

# Логи конкретного сервиса
kubectl logs -f -n mybank deploy/mybank-front-ui

# Ingress работает?
kubectl get ingress -n mybank
```

Откройте в браузере:
- **Приложение**: http://mybank.local
- **Keycloak Admin**: http://keycloak.mybank.local (admin / admin)

Тестовые пользователи: `alice / alice123`, `bob / bob123`

## Шаг 4. Обновление и удаление

```bash
# Обновить значения
helm upgrade mybank . -n mybank --set accounts-service.replicaCount=2

# Удалить
helm uninstall mybank -n mybank

# Очистить PVC (данные PostgreSQL)
kubectl delete pvc -n mybank --all
```

## Структура Helm-чартов

```
mybank-chart/                    # Зонтичный чарт
├── Chart.yaml                   # Описание + зависимости (сабчарты)
├── values.yaml                  # Глобальные настройки
├── templates/
│   └── NOTES.txt                # Инструкции после деплоя
└── charts/
    ├── postgresql/              # БД (StatefulSet)
    │   ├── Chart.yaml
    │   ├── values.yaml
    │   └── templates/
    │       ├── statefulset.yaml
    │       ├── service.yaml
    │       └── secret.yaml
    ├── keycloak/                # OAuth 2.0 Server
    │   ├── Chart.yaml
    │   ├── values.yaml
    │   ├── realm-mybank.json    # Импорт realm
    │   └── templates/
    │       ├── deployment.yaml
    │       ├── service.yaml
    │       ├── configmap-realm.yaml
    │       └── ingress.yaml
    ├── gateway-service/         # API Gateway
    │   ├── Chart.yaml
    │   ├── values.yaml
    │   └── templates/
    │       ├── deployment.yaml
    │       ├── service.yaml
    │       └── configmap.yaml
    ├── front-ui/                # Web UI + Ingress
    │   ├── Chart.yaml
    │   ├── values.yaml
    │   └── templates/
    │       ├── deployment.yaml
    │       ├── service.yaml
    │       ├── configmap.yaml
    │       └── ingress.yaml
    └── accounts-service/        # Микросервис счетов
        ├── Chart.yaml
        ├── values.yaml
        └── templates/
            ├── deployment.yaml
            ├── service.yaml
            ├── configmap.yaml
            └── secret.yaml
```

## Добавление новых микросервисов

Для cash-service, transfer-service, notifications-service — скопируйте accounts-service и измените:

1. `Chart.yaml` — имя и описание
2. `values.yaml` — образ, client-secret, schema
3. `templates/configmap.yaml` — конфигурация сервиса
4. `templates/secret.yaml` — секреты
5. В `mybank-chart/Chart.yaml` — добавьте dependency
6. В `mybank-chart/values.yaml` — добавьте секцию включения

Пример для cash-service:
```bash
cp -r charts/accounts-service charts/cash-service
# Отредактировать файлы, заменив accounts → cash, порт, schema, secrets
```

## Keycloak: проблема issuer-uri

В K8s браузер обращается к Keycloak через Ingress (`http://keycloak.mybank.local`),
а бэкенд-сервисы — через внутренний DNS (`http://mybank-keycloak:8080`).

Токен содержит `iss: http://keycloak.mybank.local/realms/mybank`, поэтому
бэкенд-сервисы используют `jwk-set-uri` (прямой URL к ключам) вместо `issuer-uri`,
чтобы избежать ошибки issuer mismatch.

Для OAuth2 Client Credentials (межсервисные вызовы) `issuer-uri` указывает
на внутренний URL Keycloak, т.к. это серверное взаимодействие без браузера.

## Troubleshooting

**Pod не стартует (CrashLoopBackOff):**
```bash
kubectl logs -n mybank <pod-name> --previous
kubectl describe pod -n mybank <pod-name>
```

**Keycloak не импортирует realm:**
```bash
kubectl exec -n mybank deploy/mybank-keycloak -- ls /opt/keycloak/data/import/
```

**Front-ui не может подключиться к gateway:**
```bash
kubectl exec -n mybank deploy/mybank-front-ui -- curl -s http://mybank-gateway-service:8090/actuator/health
```

**Ingress не работает:**
```bash
kubectl get ingress -n mybank
kubectl get pods -n ingress-nginx
# Убедитесь, что /etc/hosts настроен
```

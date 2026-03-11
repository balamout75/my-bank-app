# 🔧 MyBank — CI/CD с Jenkins

[← Назад к основному README](../README.md)

---

## Обзор

Jenkins автоматизирует полный цикл доставки: тесты → сборка Docker-образов → push в GitHub Container Registry (GHCR) → деплой в TEST → Helm Tests → ручное подтверждение → деплой в PROD → Helm Tests.

```
Developer                    Jenkins (Docker)                      Kubernetes
    │                             │                                     │
    │  git push                   │                                     │
    ├────────────────────────────▶│                                     │
    │                             │  1. mvn clean install (тесты)       │
    │                             │  2. docker build (6 образов ∥)      │
    │                             │  3. docker push → GHCR              │
    │                             │  4. helm deploy → mybank-test ─────▶│
    │                             │  5. helm test TEST                  │
    │                             │  6. ⏸ Manual Approval               │
    │                             │  7. helm deploy → mybank-prod ─────▶│
    │                             │  8. helm test PROD                  │
    │                             │                                     │
```

---

## Предусловия среды

> ⚠️ Перед первым запуском пайплайна кластер должен быть подготовлен:

**1. Ingress-nginx установлен:**
```bash
kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/controller-v1.12.0/deploy/static/provider/cloud/deploy.yaml
kubectl get pods -n ingress-nginx -w  # ждать 1/1 Running
```

**2. Мониторинг задеплоен** (ServiceMonitor CRD нужны до деплоя mybank):
```bash
cd k8s/monitoring
helm install k8s-monitoring . -n monitoring --create-namespace -f values-local.yaml
```
→ см. [k8s/monitoring/README.md](../k8s/monitoring/README.md)

**3. CoreDNS rewrite настроен** для всех окружений → см. [Шаг 3](#шаг-3-инфраструктура-kubernetes)

**4. /etc/hosts содержит записи** для всех окружений → см. [Шаг 3](#шаг-3-инфраструктура-kubernetes)

> 💡 Если уже выполнили развёртывание по инструкции `k8s/mybank/README.md` — Ingress и CoreDNS уже настроены, добавьте только записи для test/prod окружений.

---

## Окружения

| Окружение | Namespace | Release | Приложение | Keycloak |
|-----------|-----------|---------|-----------|----------|
| TEST | mybank-test | mybank-test | http://mybank.test.local | http://keycloak.mybank.test.local |
| PROD | mybank-prod | mybank-prod | http://mybank.prod.local | http://keycloak.mybank.prod.local |

---

## Структура

```
jenkins/
├── Dockerfile                          # Jenkins с helm, kubectl, docker, maven
├── docker-compose.yml                  # Запуск Jenkins контейнера
├── .env                                # Секреты (НЕ коммитить!)
├── .env.example                        # Шаблон секретов
├── plugins.txt                         # Плагины Jenkins
├── Jenkinsfile                         # Pipeline (с shared functions)
└── init.groovy.d/
    ├── 01_secure-credentials.groovy    # Автосоздание credentials из ENV
    └── 02_create-multibranch-job.groovy # Автосоздание multibranch pipeline
```

---

## Как это работает

### Jenkins в Docker

Jenkins запускается как Docker-контейнер с предустановленными инструментами:
- **helm** — деплой Helm-чартов
- **kubectl** — управление Kubernetes
- **docker CLI** — сборка и push образов (через Docker socket хоста)
- **maven** — компиляция и тесты

### Автоконфигурация при старте

При первом запуске Groovy-скрипты автоматически:
1. **01_secure-credentials.groovy** — создаёт Jenkins Credentials из переменных окружения (`.env`):
   - GitHub (username, token, GHCR token, registry)
   - Database password
   - Keycloak client secrets (accounts, cash, transfer, frontend)
   - Keycloak admin password
2. **02_create-multibranch-job.groovy** — создаёт Multibranch Pipeline проект, привязанный к GitHub-репозиторию

Jenkins сканирует все ветки репозитория и ищет `jenkins/Jenkinsfile`. Если находит — запускает pipeline.

### Pipeline (Jenkinsfile)

Jenkinsfile использует **shared functions** для устранения дублирования:

- `helmDeploy(namespace, appHost, keycloakHost)` — генерирует `--set` аргументы в цикле для всех 6 сервисов
- `helmTest(namespace)` — удаляет старые test-поды и запускает `helm test`
- Сборка Docker-образов выполняется **параллельно** (`parallel builds`)

```
┌─────────────────────────────────────────────────────────────┐
│  Build & Test                                               │
│  mvn clean install (компиляция + unit-тесты)          │
├─────────────────────────────────────────────────────────────┤
│  Build & Push Docker Images (parallel)                      │
│  docker build × 6 сервисов параллельно → GHCR               │
│  тег = BUILD_NUMBER                                         │
├─────────────────────────────────────────────────────────────┤
│  Prepare Helm                                               │
│  helm dependency update k8s/mybank                          │
├─────────────────────────────────────────────────────────────┤
│  Helm Deploy to TEST (namespace: mybank-test)               │
│  helmDeploy('mybank-test', ...) → --wait --timeout 15m      │
├─────────────────────────────────────────────────────────────┤
│  Verify TEST                                                │
│  kubectl get pods -n mybank-test                            │
│  helmTest('mybank-test')                                    │
├─────────────────────────────────────────────────────────────┤
│  ⏸ Manual Approval                                          │
│  "Deploy to PROD environment?" → [Yes, deploy]              │
├─────────────────────────────────────────────────────────────┤
│  Helm Deploy to PROD (namespace: mybank-prod)               │
│  helmDeploy('mybank-prod', ...) → --wait --timeout 15m      │
├─────────────────────────────────────────────────────────────┤
│  Verify PROD                                                │
│  kubectl get pods -n mybank-prod                            │
│  helmTest('mybank-prod')                                    │
└─────────────────────────────────────────────────────────────┘
```

- `IMAGE_TAG = BUILD_NUMBER` — автоинкремент Jenkins
- Один и тот же тег используется в TEST и PROD — что протестировали, то и деплоим
- Helm Tests работают в информативном режиме — при ошибке pipeline **не останавливается**, а выводит предупреждение

---

## Пошаговая настройка

### Шаг 1. GitHub токены

Нужны два токена:

**GITHUB_TOKEN** — доступ к репозиторию:
1. GitHub → Settings → Developer settings → Personal access tokens → **Fine-grained tokens**
2. Repository access: Only select repositories → ваш репозиторий
3. Permissions: Contents (Read-only), Metadata (Read-only)

**GHCR_TOKEN** — push образов в GitHub Container Registry:
1. GitHub → Settings → Developer settings → Personal access tokens → **Tokens (classic)**
2. Scopes: `write:packages`, `read:packages`, `delete:packages`

### Шаг 2. Подготовка .env

```bash
cp .env.example .env
```

Заполните `.env`:

```env
# === GitHub ===
GITHUB_USERNAME=your-github-username
GITHUB_TOKEN=github_pat_your-fine-grained-token
GITHUB_REPOSITORY=your-github-username/my-bank-app

# === GitHub Container Registry (GHCR) ===
GHCR_TOKEN=ghp_your-classic-token
DOCKER_REGISTRY=ghcr.io/your-github-username

# === Kubernetes ===
KUBECONFIG_PATH=C:/Users/your-username/.kube/config

# === Database ===
DB_PASSWORD=your-database-password

# === Keycloak Client Secrets ===
KC_SECRET_ACCOUNTS=your-accounts-client-secret
KC_SECRET_CASH=your-cash-client-secret
KC_SECRET_TRANSFER=your-transfer-client-secret
KC_SECRET_FRONTEND=your-frontend-client-secret

# === Keycloak Admin ===
KC_ADMIN_PASSWORD=your-keycloak-admin-password
```

> ⚠️ Файл `.env` добавлен в `.gitignore` — **никогда не коммитить**.

### Шаг 3. Инфраструктура Kubernetes

> ⚠️ Если уже выполнили развёртывание по `k8s/mybank/README.md` — пропустите этот шаг, всё уже настроено. Добавьте только строки для test/prod в hosts и CoreDNS.

**Ingress-nginx:**

```bash
kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/controller-v1.12.0/deploy/static/provider/cloud/deploy.yaml
kubectl get pods -n ingress-nginx -w
```

**CoreDNS rewrite:**

```bash
kubectl edit configmap coredns -n kube-system
```

Добавить перед `kubernetes cluster.local`:

```
    rewrite name keycloak.mybank.local      mybank-keycloak.mybank.svc.cluster.local
    rewrite name keycloak.mybank.dev.local  mybank-keycloak.mybank.svc.cluster.local
    rewrite name keycloak.mybank.test.local mybank-test-keycloak.mybank-test.svc.cluster.local
    rewrite name keycloak.mybank.prod.local mybank-prod-keycloak.mybank-prod.svc.cluster.local
```

```bash
kubectl rollout restart deployment coredns -n kube-system
```

**Windows hosts** (`C:\Windows\System32\drivers\etc\hosts`):

```
127.0.0.1  mybank.local           keycloak.mybank.local
127.0.0.1  mybank.dev.local       keycloak.mybank.dev.local
127.0.0.1  mybank.test.local      keycloak.mybank.test.local
127.0.0.1  mybank.prod.local      keycloak.mybank.prod.local
127.0.0.1  prometheus.monitoring.local  grafana.monitoring.local
127.0.0.1  alertmanager.monitoring.local  kibana.monitoring.local
```

> 💡 DNS настраивается один раз для всех окружений — dev, test и prod. Переключение между ручным деплоем и Jenkins не требует изменений в hosts.

### Шаг 4. Запуск Jenkins

```bash
cd jenkins
docker compose up --build
```

Дождитесь в логах:

```
--> Credential setup complete.
--> Multibranch job 'MyBank' создан и запущен на '...'
Jenkins is fully up and running
```

### Шаг 5. Запуск pipeline

1. Откройте http://localhost:8080
2. Нажмите **MyBank**
3. Нажмите **Scan Repository Now** — Jenkins найдёт Jenkinsfile
4. Ветка с Jenkinsfile появится в списке
5. Нажмите на ветку → **Build Now**
6. После TEST деплоя и Helm Tests появится кнопка **Yes, deploy** для PROD

---

## Helm Tests в pipeline

Helm Tests запускаются автоматически после деплоя в каждое окружение:

```
Helm Deploy to TEST → Verify TEST → Helm Test TEST → Manual Approval
Helm Deploy to PROD → Verify PROD → Helm Test PROD → ✅ Done
```

### Что проверяют тесты

| Тест | Проверка |
|------|---------|
| **test-health** | PostgreSQL (TCP), Keycloak (TCP), Kafka (TCP), 6 сервисов (actuator/health) |
| **test-connectivity** | Маршруты Gateway → accounts, cash, transfer, notifications |

### Информативный режим

Тесты работают в режиме `|| echo "⚠️ Some tests failed"` — при ошибке pipeline **продолжает работу** и выводит предупреждение. Это позволяет задеплоить и проверить вручную, не блокируя весь процесс.

---

## Управление секретами

### Схема прохождения секретов

```
.env (файл, НЕ коммитится)
  │
  ▼
Groovy-скрипт → Jenkins Credentials (хранятся в jenkins_home volume)
  │
  ▼
Jenkinsfile → --set keycloak.clientSecrets.* и --set *.keycloak.clientSecret
  │
  ▼
Helm → K8s Secret (keycloak-realm-secrets)
  │
  ▼
Init Container (envsubst) → realm-mybank.json в памяти (emptyDir)
  │
  ▼
Keycloak --import-realm
```

Секреты **никогда не попадают в Git**. `realm-mybank.json` в репозитории содержит только плейсхолдеры `${KC_SECRET_*}` — реальные значения подставляются при старте пода.

### В ручном K8s-деплое (DEV/LOCAL)

Для локальной разработки секреты передаются через `values-local.yaml` (добавлен в `.gitignore`):

```yaml
# k8s/mybank/values-local.yaml (НЕ коммитить!)
global:
  postgresql:
    password: "mybank_password"

accounts-service:
  keycloak:
    clientSecret: "your-accounts-secret"

cash-service:
  keycloak:
    clientSecret: "your-cash-secret"

transfer-service:
  keycloak:
    clientSecret: "your-transfer-secret"

front-ui:
  keycloak:
    clientSecret: "your-frontend-secret"

keycloak:
  admin:
    password: "admin"
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
| keycloak-realm-secrets | KC_SECRET_ACCOUNTS, KC_SECRET_CASH, KC_SECRET_TRANSFER, KC_SECRET_FRONTEND |
| postgresql | POSTGRES_DB, POSTGRES_USER, POSTGRES_PASSWORD |

> ⚠️ В `values.yaml` все секреты — пустые placeholder'ы (`""`). Реальные значения передаются через `--set` (Jenkins) или `values-local.yaml` (dev).

### Фильтр ветки для Jenkins

В `.env` задаётся переменная `BRANCH_FILTER` — имя ветки, которую Jenkins будет мониторить и деплоить. Pull request'ы (`PR-*`) отслеживаются всегда.
```
BRANCH_FILTER=sprint-12-bank-features
```

Если переменная не задана — Jenkins мониторит `main` и `master` по умолчанию. Значение подхватывается в `02_create-multibranch-job.groovy` через `System.getenv()`.

> ⚠️ После смены `BRANCH_FILTER` в `.env` необходимо пересоздать Jenkins-контейнер — init-скрипты выполняются только при старте.

---

## Docker-образы

Образы хранятся в GitHub Container Registry (GHCR):

```
ghcr.io/your-username/mybank-accounts:BUILD_NUMBER
ghcr.io/your-username/mybank-cash:BUILD_NUMBER
ghcr.io/your-username/mybank-transfer:BUILD_NUMBER
ghcr.io/your-username/mybank-notifications:BUILD_NUMBER
ghcr.io/your-username/mybank-gateway:BUILD_NUMBER
ghcr.io/your-username/mybank-frontend:BUILD_NUMBER
```

Посмотреть: `https://github.com/your-username?tab=packages`

---

## Управление Jenkins

### Перезапуск с сохранением данных

```bash
docker compose restart
```

### Полный сброс (удаление volume, пересоздание credentials)

```bash
docker compose down -v
docker compose up --build
```

### Очистка зависших Helm-релизов

```bash
helm uninstall mybank-test -n mybank-test
helm uninstall mybank-prod -n mybank-prod
```

### Остановка Jenkins

```bash
docker compose down
```

---

## Отличия от ручного K8s-деплоя

| Аспект | K8s (ручной) | Jenkins CI/CD |
|--------|-------------|---------------|
| Тесты (Java) | Не запускаются | `mvn clean install -T 1C` |
| Сборка | `docker buildx bake` (локально) | `docker build` параллельно в Jenkins |
| Образы | Локальные | GHCR (ghcr.io) |
| Деплой | `helm install` вручную | Автоматически при push |
| Секреты | `values-local.yaml` | `.env` → Jenkins Credentials → `--set` |
| Окружения | Одно (mybank) | mybank-test + mybank-prod |
| Подтверждение | Нет | Manual Approval перед PROD |
| Тег образа | latest | BUILD_NUMBER (версионирование) |
| Helm Tests | `helm test` вручную | Автоматически после каждого деплоя |
| Откат | `helm rollback` | Запуск предыдущего билда |

---

## Частые проблемы

| Проблема | Причина | Решение |
|----------|---------|---------|
| `Bad credentials (401)` | Токен отозван/истёк | Пересоздать токен, обновить `.env`, `docker compose down -v && up --build` |
| `Jenkinsfile not found` | Файл не запушен | `git add jenkins/Jenkinsfile && git push` |
| `another operation in progress` | Зависший Helm релиз | `helm uninstall mybank-test -n mybank-test` |
| `context deadline exceeded` | Поды не стартуют | `kubectl logs -n mybank-test <pod>` |
| `admission webhook denied` | Конфликт Ingress хостов | Разные release names и хосты для TEST/PROD |
| `Push blocked` | Секреты в Git | `git rm --cached jenkins/.env`, добавить в `.gitignore` |
| Compilation error `\` | Пробел после `\` в Jenkinsfile | Убрать trailing spaces |
| Helm Test Failed | Старые test-поды | Pipeline автоматически удаляет их перед запуском |
| `localhost:9092` в логах Kafka | Нет KAFKA_BOOTSTRAP_SERVERS | Проверить secret.yaml сервиса и configmap |
| `no main manifest attribute` | Нет spring-boot-maven-plugin | Добавить плагин в pom.xml сервиса |
| Keycloak `Init:ErrImagePull` | Образ envsubst недоступен | Init container использует `eclipse-temurin:21-jre-alpine` — должен быть в кэше |
| Keycloak `Init:CrashLoopBackOff` | Secret `keycloak-realm-secrets` не найден | Проверить что `keycloak-realm-secret.yaml` задеплоен в нужный namespace |

# MyBank — Полное развёртывание с нуля

Инструкция для ревьюера: от пустого компьютера до работающего приложения.

---

## Предварительные требования

| Компонент | Версия                   | Проверка |
|-----------|--------------------------|---------|
| Docker Desktop | 4.x+                     | `docker --version` |
| Kubernetes | включён в Docker Desktop | `kubectl cluster-info` |
| Helm | 4.x                     | `helm version` |
| Java | 21                       | `java -version` |
| Maven | 3.9+                     | `mvn -version` |
| Git | любой                    | `git --version` |

### Включение Kubernetes в Docker Desktop

Settings → Kubernetes → ✅ Enable Kubernetes → Apply & Restart.
Дождаться зелёного индикатора "Kubernetes is running".

---

## Шаг 1. Клонирование репозитория

```bash
git clone https://github.com/<username>/my-bank-app.git
cd my-bank-app
```

---

## Шаг 2. Установка Ingress-nginx

```bash
helm repo add ingress-nginx https://kubernetes.github.io/ingress-nginx
helm repo update
helm install ingress-nginx ingress-nginx/ingress-nginx \
  --namespace ingress-nginx --create-namespace \
  --set controller.service.type=LoadBalancer
```

Проверка:

```bash
kubectl get pods -n ingress-nginx
```

Дождаться статуса `Running` у пода `ingress-nginx-controller-*`.

---

## Шаг 3. Настройка DNS (hosts файл)

Добавить в hosts файл:

**Windows** (`C:\Windows\System32\drivers\etc\hosts`) — от имени администратора:

```
127.0.0.1 mybank.dev.local keycloak.mybank.dev.local
```

**macOS / Linux** (`/etc/hosts`):

```
127.0.0.1 mybank.dev.local keycloak.mybank.dev.local
```

---

## Шаг 4. Сборка проекта (Maven)

```bash
mvn clean install
```

Это выполнит:
- компиляцию всех модулей
- unit-тесты (Mockito)
- генерацию и установку стабов (Spring Cloud Contract)
- интеграционные тесты (Testcontainers + PostgreSQL)
- Kafka IT тесты (EmbeddedKafka)
- consumer контрактные тесты (Stub Runner)

Ожидаемый результат:

```
[INFO] BUILD SUCCESS
[INFO] Total time: ~3-5 min
```

Если Testcontainers-тесты падают (нет Docker), можно пропустить IT:

```bash
mvn clean install -DskipITs
```

---

## Шаг 5. Сборка Docker-образов

### Вариант A: через BuildKit (Maven не нужен на машине)

```bash
docker buildx bake --load -f docker-bake.hcl
```

### Вариант B: через Dockerfile.ci (JAR уже собран на шаге 4)

```bash
docker build -f Dockerfile.ci --build-arg MODULE=accounts-service -t mybank/yp-mybank-accounts:latest .
docker build -f Dockerfile.ci --build-arg MODULE=cash-service -t mybank/yp-mybank-cash:latest .
docker build -f Dockerfile.ci --build-arg MODULE=transfer-service -t mybank/yp-mybank-transfer:latest .
docker build -f Dockerfile.ci --build-arg MODULE=notifications-service -t mybank/yp-mybank-notifications:latest .
docker build -f Dockerfile.ci --build-arg MODULE=gateway-service -t mybank/yp-mybank-gateway:latest .
docker build -f Dockerfile.ci --build-arg MODULE=front-ui -t mybank/yp-mybank-frontend:latest .
```

Проверка:

```bash
docker images | grep mybank
```

Должно быть 6 образов.

---

## Шаг 6. Подготовка секретов

```bash
cp k8s/values-local.yaml.example k8s/values-local.yaml
```

Содержимое `k8s/values-local.yaml`:

```yaml
global:
  postgresql:
    password: "mybank_password"

accounts-service:
  keycloak:
    clientSecret: "Ycm7AwLxKchJ76kaRwDaG0RyHsp1T2rK"

cash-service:
  keycloak:
    clientSecret: "GJj0e0li8KrcaHb9S20ze8SDo8lO1zIL"

transfer-service:
  keycloak:
    clientSecret: "RnGnXR7iX8ZSgb5lSiiG53QteuGrKC9h"

front-ui:
  keycloak:
    clientSecret: "lLL8B1F0WroO7QXWC58nzfR2OZ4fUp9q"

keycloak:
  admin:
    password: "admin"
```

⚠️ Этот файл **НЕ** коммитится в Git (добавлен в `.gitignore`).

---

## Шаг 7. Деплой в Kubernetes

```bash
cd k8s
helm dependency update .
helm upgrade --install mybank . -n mybank --create-namespace -f values-local.yaml
```

Ожидание запуска подов (может занять 5-10 минут):

```bash
kubectl get pods -n mybank -w
```

Ожидаемый результат — все поды в статусе `Running`:

```
NAME                                         READY   STATUS    
mybank-accounts-service-xxx                  1/1     Running
mybank-cash-service-xxx                      1/1     Running
mybank-transfer-service-xxx                  1/1     Running
mybank-notifications-service-xxx             1/1     Running
mybank-gateway-service-xxx                   1/1     Running
mybank-front-ui-xxx                          1/1     Running
mybank-keycloak-xxx                          1/1     Running
mybank-postgresql-0                          1/1     Running
mybank-kafka-0                               1/1     Running
```

Если поды рестартятся — это нормально (Keycloak и PostgreSQL стартуют долго, остальные ждут).

---

## Шаг 8. Helm Tests

```bash
helm test mybank -n mybank
```

Проверяет health endpoints и сетевую связность между сервисами.

---

## Шаг 9. Проверка приложения

Открыть в браузере:

```
http://mybank.dev.local
```

1. Произойдёт редирект на Keycloak (`keycloak.mybank.dev.local`)
2. Войти: **alice / alice**
3. После входа — дашборд с балансом

### Проверка основных сценариев

**Пополнение:**
- Перейти в Cash → Deposit
- Указать сумму → Submit
- Баланс должен увеличиться

**Перевод:**
- Перейти в Transfer
- Выбрать получателя (Bob), указать сумму → Submit
- Баланс Alice уменьшится, баланс Bob увеличится

**Kafka-уведомления (проверка логов):**

```bash
kubectl logs -n mybank deployment/mybank-notifications-service --tail=20
```

Ожидаемое:

```
NOTIFICATION CREATED: service=cash-service, opId=1
NOTIFICATION CREATED: service=transfer-service, opId=1
```

---

## Шаг 10. Проверка тестов (детально)

### Unit + Integration тесты

```bash
mvn clean install
```

### Только unit тесты (быстро)

```bash
mvn test
```

### Только IT тесты определённого модуля

```bash
mvn verify -pl cash-service
mvn verify -pl notifications-service
```

### Сводка тестов

| Сервис | Unit | Integration | Kafka IT | Contract | Итого |
|--------|------|-------------|----------|----------|-------|
| Cash Service | 3+ | 4 | 1 | 3 provider | 11+ |
| Transfer Service | 3+ | 4 | 1 | — | 8+ |
| Accounts Service | 3+ | — | 1 | 3 provider | 7+ |
| Notifications Service | 7 | 2 | — | — | 9 |
| Front UI | 5 | 8 | — | 3 consumer | 16 |
| **Итого** | **21+** | **18+** | **3** | **9** | **51+** |

---

## Полезные команды

### Диагностика

```bash
# Статус подов
kubectl get pods -n mybank

# Логи конкретного сервиса
kubectl logs -n mybank deployment/mybank-cash-service --tail=50

# Описание пода (события, ошибки)
kubectl describe pod <pod-name> -n mybank

# Проверка секретов
kubectl get secret mybank-accounts-service-secret -n mybank -o jsonpath='{.data}' | \
  powershell -Command "[System.Text.Encoding]::UTF8.GetString([System.Convert]::FromBase64String((Get-Content -Raw)))"

# Проверка Kafka
kubectl exec -n mybank mybank-kafka-0 -- kafka-topics.sh --list --bootstrap-server localhost:9092
```

### Очистка

```bash
# Удалить деплой
helm uninstall mybank -n mybank

# Удалить namespace (всё, включая PVC)
kubectl delete namespace mybank

# Удалить Docker-образы
docker rmi $(docker images | grep mybank | awk '{print $3}')
```

### Переустановка с нуля

```bash
helm uninstall mybank -n mybank
kubectl delete namespace mybank
kubectl create namespace mybank
helm dependency update k8s
helm upgrade --install mybank k8s -n mybank --create-namespace -f k8s/values-local.yaml
kubectl get pods -n mybank -w
```

---

## Частые проблемы

| Проблема | Причина | Решение |
|----------|---------|---------|
| `CrashLoopBackOff` у сервисов | Keycloak ещё не стартовал | Подождать 3-5 минут, сервисы перезапустятся |
| `ImagePullBackOff` | Образ не найден локально | `docker buildx bake --load -f docker-bake.hcl` |
| `localhost:9092` в логах | Kafka bootstrap не из ConfigMap | Проверить configmap: `spring.kafka.bootstrap-servers` |
| `no main manifest attribute` | front-ui без spring-boot-maven-plugin | Проверить front-ui/pom.xml |
| Consumer тесты: stubs not found | Стабы не в ~/.m2 | Проверить `<extensions>true</extensions>` в SCC plugin |
| ERR_NAME_NOT_RESOLVED | Нет записи в hosts | Добавить `127.0.0.1 mybank.dev.local keycloak.mybank.dev.local` |
| Helm timeout | Медленный старт на локальном K8s | Увеличить `--timeout 15m` |

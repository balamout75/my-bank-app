# 📊 MyBank — Observability стек в Kubernetes

[← Назад к README](../../README.md)

---

## Обзор

Helm-чарт `k8s/monitoring` разворачивает полный observability-стек в namespace `monitoring`.
Чарт полностью независим от `k8s/mybank` — деплоится отдельно и живёт своей жизнью.

| Компонент | Назначение | URL |
|-----------|-----------|-----|
| **Prometheus** | Сбор и хранение метрик | http://prometheus.monitoring.local |
| **Grafana** | Дашборды и визуализация | http://grafana.monitoring.local |
| **Alertmanager** | Маршрутизация алертов | http://alertmanager.monitoring.local |
| **Kibana** | Поиск, анализ, дашборды | http://kibana.monitoring.local |
| **Elasticsearch** | Хранение и индексирование логов | ClusterIP :9200 (внутри кластера) |
| **Logstash** | Парсинг и обогащение логов | ClusterIP :5044/:5000 (внутри кластера) |
| **Filebeat** | Сбор логов с подов (DaemonSet) | — |

---

## Архитектура

```
namespace: mybank                     namespace: monitoring
─────────────────────────             ──────────────────────────────────────
Spring Boot pods                      ┌─────────────────────────────────────┐
  ├── ECS JSON → stdout               │                                     │
  └── /actuator/prometheus            │  Filebeat (DaemonSet)               │
                                      │    └── читает /var/log/containers/  │
Метрики:                              │         (только namespace: mybank)  │
  ServiceMonitor ──────────────────→  │  Logstash :5044                     │
  (CRD из kube-prometheus-stack)      │    └── парсинг, PII маскировка      │
                                      │  Elasticsearch                      │
                                      │    └── индекс: monitoring-logs-*    │
                                      │  Kibana                             │
                                      │    └── Discover, Dashboards         │
                                      │                                     │
                                      │  Prometheus (kube-prometheus-stack) │
                                      │    ├── scrape via ServiceMonitors   │
                                      │    └── PrometheusRule alerts        │
                                      │  Grafana                            │
                                      │  Alertmanager                       │
                                      └─────────────────────────────────────┘
```

**Поток логов:**
```
Pod stdout (ECS JSON)
  → Filebeat (DaemonSet, /var/log/containers/)
  → Logstash :5044
    ├── нормализация namespace, service_name, level
    ├── парсинг событий notifications-service (grok)
    ├── PII маскировка (имена, суммы)
    └── фильтрация шума (Kafka internal, ActiveMQ)
  → Elasticsearch (monitoring-logs-YYYY.MM.dd)
  → Kibana
```

**Поток метрик:**
```
Spring Boot /actuator/prometheus
  → ServiceMonitor (CRD, namespace: mybank)
  → Prometheus (scrape каждые 15 сек)
  → PrometheusRule alerts → Alertmanager
  → Grafana dashboards
```

---

## Структура чарта

```
k8s/monitoring/
├── Chart.yaml                   # Зонтичный чарт + зависимость kube-prometheus-stack
├── values.yaml                  # Глобальные настройки
├── values-local.yaml            # Переопределения для docker-desktop
├── templates/
│   ├── ingress.yaml             # Ingress для всех UI-компонентов
│   └── NOTES.txt                # Инструкции после деплоя
├── charts/
│   ├── elasticsearch/
│   ├── logstash/
│   │   └── templates/
│   │       └── configmap.yaml   # Logstash pipeline — парсинг и маскировка
│   ├── kibana/
│   └── filebeat/
└── kibana/
    └── notifications-dashboard.ndjson  # Дашборд для импорта
```

---

## Предусловия

- Docker Desktop с включённым Kubernetes
- Helm v4+
- kubectl
- Ingress-nginx установлен в кластере
- Доступ к интернету (для скачивания `kube-prometheus-stack`)

---

## Установка

> ⚠️ Все команды выполняются из папки `k8s/monitoring/`, если не указано иное.

### Шаг 1. Ingress-nginx

```bash
kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/controller-v1.12.0/deploy/static/provider/cloud/deploy.yaml

# Ждать 1/1 Running
kubectl get pods -n ingress-nginx -w
```

### Шаг 2. DNS — /etc/hosts

**Windows** — `C:\Windows\System32\drivers\etc\hosts` (от имени администратора)
**Linux / macOS** — `/etc/hosts`

```
127.0.0.1  prometheus.monitoring.local  grafana.monitoring.local
127.0.0.1  alertmanager.monitoring.local  kibana.monitoring.local
```

### Шаг 3. Helm-репозиторий

```bash
helm repo add prometheus-community https://prometheus-community.github.io/helm-charts
helm repo update
```

### Шаг 4. Скачать зависимости

```bash
cd k8s/monitoring
helm dependency update
```

### Шаг 5. values-local.yaml

Для docker-desktop необходимо отключить компоненты control plane которых нет в docker-desktop:

```yaml
# k8s/monitoring/values-local.yaml
kube-prometheus-stack:
  kubeEtcd:
    enabled: false
  kubeControllerManager:
    enabled: false
  kubeScheduler:
    enabled: false
  kubeProxy:
    enabled: false
  prometheus:
    prometheusSpec:
      serviceMonitorSelectorNilUsesHelmValues: false
      podMonitorSelectorNilUsesHelmValues: false
      resources:
        requests:
          memory: "512Mi"
        limits:
          memory: "1Gi"
  grafana:
    resources:
      requests:
        memory: "128Mi"
      limits:
        memory: "256Mi"
```

### Шаг 6. Установить чарт

```bash
cd k8s/monitoring
helm dependency update
helm upgrade --install k8s-monitoring . -n monitoring --create-namespace -f values-local.yaml

ну, или при первой установке 
helm upgrade --install k8s-monitoring . -n monitoring --create-namespace -f values-local.yaml
```

### Шаг 7. Проверить готовность

```bash
kubectl get pods -n monitoring -w
```

Ожидаемый порядок готовности: Elasticsearch → Prometheus/Grafana → Logstash → Filebeat → Kibana.

```
elasticsearch-0                                          1/1  Running  ← первым
logstash-xxxxx                                           1/1  Running
filebeat-xxxxx                                           1/1  Running
k8s-monitoring-grafana-xxxxx                             1/1  Running
k8s-monitoring-kube-promet-operator-xxxxx                1/1  Running
k8s-monitoring-kube-promet-prometheus-xxxxx              1/1  Running
k8s-monitoring-kube-promet-alertmanager-xxxxx            1/1  Running
k8s-monitoring-kube-state-metrics-xxxxx                  1/1  Running
kibana-xxxxx                                             1/1  Running  ← последним, ~2-4 мин
```

> ⚠️ **Kibana стартует 2–4 минуты.** Кратковременный `CrashLoopBackOff` при старте — нормально, Kibana ждёт Elasticsearch. Дождитесь `1/1 Running`.

---

## Доступ к интерфейсам

| Сервис | URL | Credentials |
|--------|-----|-------------|
| Prometheus | http://prometheus.monitoring.local | — |
| Grafana | http://grafana.monitoring.local | admin / admin |
| Alertmanager | http://alertmanager.monitoring.local | — |
| Kibana | http://kibana.monitoring.local | — |

> ⚠️ **Zipkin** находится в чарте `k8s/mybank` (namespace: mybank), не здесь.

---

## Настройка Kibana после установки

### 1. Создать Data View

```
Stack Management → Data Views → Create data view
```

| Поле | Значение |
|------|---------|
| Name | mybank-logs |
| Index pattern | `monitoring-logs-*` |
| Timestamp field | `@timestamp` |

### 2. Настроить колонки в Discover

```
Analytics → Discover
```

Добавить колонки: `kubernetes.namespace`, `service_name`, `log.level`, `message`, `kubernetes.pod.name`

Сохранить как: **mybank-logs**

### 3. Импортировать дашборды

```
Stack Management → Saved Objects → Import
```

| Файл | Описание |
|------|---------|
| `kibana/notifications-dashboard.ndjson` | Операции через notifications-service |

## Полезные KQL-запросы
```kql
# Только ошибки
log.level: "ERROR"

# Логи конкретного сервиса
service.name: "accounts-service"

# Все уведомления
service.name: "notifications-service" AND message: *NOTIFIED*

# Переводы
service.name: "notifications-service" AND message: *TRANSFER*

# Ошибки notifications
service.name: "notifications-service" AND message: *ERROR*

# По trace ID
traceId: "abc123"

# По namespace — фильтрация по среде (dev / test / prod)
kubernetes.namespace: "mybank"
```

> **Фильтр по `kubernetes.namespace`** особенно важен при использовании Dashboard:
> без него все среды (`mybank`, `mybank-test`, `mybank-prod`) попадают в одну выборку,
> и счётчики уведомлений, ошибок и операций суммируются по всем окружениям сразу.
> Добавив этот фильтр в Dashboard как глобальный, вы получаете изолированный
> срез метрик для каждой среды — что критично при параллельном деплое в test и prod.

---

## Logstash Pipeline

Получает логи от Filebeat по beats-протоколу (:5044) и выполняет:

**Порядок обработки:**
1. JSON fallback — повторный парсинг plain-text строк (JVM startup messages)
2. Нормализация `namespace` из `kubernetes.namespace` — **первым**, до всех фильтров
3. Нормализация `service_name` из `service.name` или `kubernetes.labels.app`
4. Нормализация `level` из `log.level`
5. Фильтрация шума (Kafka internal, ActiveMQ)
6. Парсинг событий `notifications-service` (grok) → поля `notif_event`, `notif_operation_type`
7. PII маскировка (только namespace: mybank)

**Парсируемые события notifications-service:**

| notif_event | Сообщение |
|-------------|-----------|
| RECEIVED | `📩 KAFKA RECEIVED: service=..., opId=..., user=...` |
| CREATED | `✅ NOTIFICATION CREATED: service=..., opId=..., user=...` |
| SKIPPED | `⏭️ NOTIFICATION SKIPPED (duplicate): service=..., opId=...` |
| NOTIFIED | `🚀✅ NOTIFIED opId=... user=... service=... payload={...}` |
| RETRY | `🚀⚠️ RETRY opId=... user=... service=... attempt=...` |
| FAILED | `🚀💥 NOTIFICATION FAILED opId=... user=... service=...` |
| ERROR | `❌ NOTIFICATION ERROR: service=..., opId=..., error=...` |

**Тип операции** (`notif_operation_type`) определяется по `notif_source_service` и содержимому payload:

| notif_operation_type | Условие |
|---------------------|---------|
| ACCOUNT_UPDATE | source = accounts-service |
| WITHDRAW | source = cash-service + payload содержит WITHDRAW |
| DEPOSIT | source = cash-service + payload содержит DEPOSIT |
| TRANSFER | source = transfer-service |

**PII маскировка** (namespace: mybank):
- Имена в message: `firstName`, `lastName`, `fullName` и др. → `[MASKED]`
- Суммы в message: `amount`, `balance`, `credit` и др. → `***`
- ECS поля: `user.full_name`, `user.name` → `[MASKED]`; `transaction.amount` → `***`

---

## Управление чартом

### Обновление

```bash
cd k8s/monitoring
helm upgrade k8s-monitoring . -n monitoring -f values-local.yaml
```

### Перезапуск компонента

```bash
kubectl rollout restart deployment/logstash -n monitoring
kubectl rollout restart deployment/kibana -n monitoring
```

### Удаление

```bash
helm uninstall k8s-monitoring -n monitoring

# Удалить PVC Elasticsearch (данные логов)
kubectl delete pvc -n monitoring -l app=elasticsearch
```

---

## Диагностика

### Метрики не собираются (targets DOWN в Prometheus)

```bash
# Проверить ServiceMonitor-ы
kubectl get servicemonitor -n mybank

# Убедиться что в values-local.yaml есть:
# serviceMonitorSelectorNilUsesHelmValues: false
```

### Логи не попадают в Kibana

```bash
# 1. Filebeat отправляет?
kubectl logs -n monitoring daemonset/filebeat --tail=30

# 2. Logstash получает?
kubectl logs -n monitoring deployment/logstash --tail=50

# 3. Elasticsearch принимает?
kubectl exec -n monitoring deployment/logstash -- \
  curl -s http://elasticsearch-service:9200/_cat/indices?v
```

### Kibana OOMKilled

```yaml
# values.yaml — увеличить лимит:
kibana:
  resources:
    limits:
      memory: "1.5Gi"   # меньше 1Gi → OOMKilled
```

### После helm upgrade pod не перезапустился

Helm обновляет ConfigMap, но не рестартует pod автоматически:

```bash
kubectl rollout restart deployment/logstash -n monitoring
```

---

## Известные особенности

| Проблема | Статус | Комментарий |
|----------|--------|-------------|
| `node-exporter` CrashLoopBackOff | ⚠️ Известная | На docker-desktop проблема с `/sys/fs/cgroup`. Не влияет на остальное |
| Kibana медленный старт | ℹ️ Норма | Первый запуск 2–4 минуты |
| Logstash ERROR в логах про `http://elasticsearch:9200` | ℹ️ Норма | X-Pack license checker — не мешает pipeline, данные идут через `elasticsearch-service:9200` |

---

## values.yaml — ключевые параметры

```yaml
# Ingress — хосты для UI компонентов
ingress:
  hosts:
    prometheus:   prometheus.monitoring.local
    grafana:      grafana.monitoring.local
    alertmanager: alertmanager.monitoring.local
    kibana:       kibana.monitoring.local

kube-prometheus-stack:
  prometheus:
    prometheusSpec:
      serviceMonitorSelectorNilUsesHelmValues: false  # обязательно!
      podMonitorSelectorNilUsesHelmValues: false
  grafana:
    adminPassword: admin

elasticsearch:
  persistence:
    size: 10Gi
  javaOpts: "-Xms512m -Xmx512m"

kibana:
  resources:
    limits:
      memory: "1.5Gi"   # меньше → OOMKilled

filebeat:
  watchNamespace: "mybank"   # собирать логи только из mybank
                              # "" = все namespace (для prod)
```

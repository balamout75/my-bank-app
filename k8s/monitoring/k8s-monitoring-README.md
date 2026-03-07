# 📊 MyBank — Observability стек в Kubernetes

[← Назад к README](../../README.md)

---

## Обзор

Helm-чарт `k8s/monitoring` разворачивает полный observability-стек в namespace `monitoring`.
Чарт полностью независим от `k8s/mybank` — деплоится отдельно и живёт своей жизнью.

| Компонент | Назначение | Порт |
|-----------|-----------|------|
| **Prometheus** | Сбор и хранение метрик | 9090 |
| **Grafana** | Дашборды и визуализация | 3000 |
| **Alertmanager** | Маршрутизация алертов | 9093 |
| **Elasticsearch** | Хранение и индексирование логов | 9200 |
| **Logstash** | Парсинг и обогащение логов | 5044 (beats), 5000 (tcp) |
| **Kibana** | Поиск, анализ, визуализация логов | 5601 |
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
  ServiceMonitor ──────────────────→  │  Logstash                           │
  (CRD из kube-prometheus-stack)      │    └── парсинг, фильтрация, enrich  │
                                      │  Elasticsearch                      │
                                      │    └── индекс: monitoring-logs-*    │
                                      │  Kibana                             │
                                      │    └── Discover, Dashboards         │
                                      │                                     │
                                      │  Prometheus (kube-prometheus-stack) │
                                      │    ├── scrape via ServiceMonitors   │
                                      │    └── PrometheusRule alerts        │
                                      │  Grafana                            │
                                      │    └── дашборды (dashboards 19004)  │
                                      │  Alertmanager                       │
                                      └─────────────────────────────────────┘
```

**Поток логов:**
```
Pod (stdout, ECS JSON)
  → Filebeat (DaemonSet, /var/log/containers/)
  → Logstash (beats protocol :5044)
  → Elasticsearch (index: monitoring-logs-YYYY.MM.dd)
  → Kibana
```

**Поток метрик:**
```
Spring Boot /actuator/prometheus
  → ServiceMonitor (CRD, namespace: mybank)
  → Prometheus scrape (каждые 15 сек)
  → PrometheusRule alerts → Alertmanager
  → Grafana dashboards
```

---

## Структура чарта

```
k8s/monitoring/
├── Chart.yaml                   # Зонтичный чарт + зависимость kube-prometheus-stack
├── values.yaml                  # Глобальные настройки всех компонентов
├── values-local.yaml            # Переопределения для docker-desktop
├── templates/
│   └── NOTES.txt                # Инструкции после деплоя
└── charts/
    ├── elasticsearch/
    │   ├── templates/
    │   │   ├── statefulset.yaml # StatefulSet + PVC (10Gi)
    │   │   └── service.yaml     # ClusterIP :9200
    │   └── values.yaml
    ├── logstash/
    │   ├── templates/
    │   │   ├── deployment.yaml
    │   │   ├── service.yaml     # ClusterIP :5044, :5000
    │   │   └── configmap.yaml   # pipeline.conf — правила парсинга
    │   └── values.yaml
    ├── kibana/
    │   ├── templates/
    │   │   ├── deployment.yaml
    │   │   └── service.yaml     # ClusterIP :5601
    │   └── values.yaml
    └── filebeat/
        ├── templates/
        │   ├── daemonset.yaml   # Запускается на каждом узле
        │   ├── configmap.yaml   # filebeat.yml — правила сбора
        │   └── rbac.yaml        # ClusterRole для чтения pod metadata
        └── values.yaml
```

> `kube-prometheus-stack` (Prometheus + Grafana + Alertmanager) указан как зависимость в `Chart.yaml` с `repository: prometheus-community/helm-charts`.

---

## Предусловия

- Docker Desktop с включённым Kubernetes
- Helm v4+
- kubectl
- Доступ к интернету (для скачивания `kube-prometheus-stack`)

---

## Установка

### Шаг 1. Добавить Helm-репозиторий

```bash
helm repo add prometheus-community https://prometheus-community.github.io/helm-charts
helm repo update
```

### Шаг 2. Скачать зависимости

```bash
cd k8s/monitoring
helm dependency update
```

### Шаг 3. Создать values-local.yaml

Для docker-desktop некоторые компоненты kube-prometheus-stack нужно отключить (они требуют доступа к компонентам control plane, которых нет в docker-desktop):

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

### Шаг 4. Установить чарт

```bash
helm install k8s-monitoring . -n monitoring --create-namespace -f values-local.yaml
```

### Шаг 5. Проверить готовность

```bash
kubectl get pods -n monitoring -w
```

Ожидаемое состояние (все `1/1 Running`):

```
elasticsearch-0                    1/1 Running
filebeat-xxxxx                     1/1 Running
kibana-xxxxx                       1/1 Running
logstash-xxxxx                     1/1 Running
k8s-monitoring-grafana-xxxxx       1/1 Running
k8s-monitoring-kube-prometheus-xxx 1/1 Running
k8s-monitoring-alertmanager-xxxxx  1/1 Running
```

> Kibana стартует дольше всего (~2 мин). Если видите CrashLoopBackOff — это нормально при первом старте, дождитесь.

---

## Доступ к интерфейсам

### Prometheus

```bash
kubectl port-forward -n monitoring svc/k8s-monitoring-kube-prometheus-prometheus 9090:9090
```
Открыть: http://localhost:9090

Полезные запросы:
```
# Все targets mybank
http://localhost:9090/targets?search=mybank

# Метрика активных алертов
http://localhost:9090/alerts
```

### Grafana

```bash
kubectl port-forward -n monitoring svc/k8s-monitoring-grafana 3000:80
```
Открыть: http://localhost:3000  (admin / admin)

Рекомендуемые дашборды (импорт через Dashboards → Import):

| ID | Название | Назначение |
|----|----------|-----------|
| 19004 | Spring Boot Statistics | Метрики Spring Boot 3.x/4.x |
| 1860 | Node Exporter Full | Метрики узлов кластера |

### Kibana

```bash
kubectl port-forward -n monitoring svc/kibana-service 5601:5601
```
Открыть: http://localhost:5601

### Alertmanager

```bash
kubectl port-forward -n monitoring svc/k8s-monitoring-kube-prometheus-alertmanager 9093:9093
```
Открыть: http://localhost:9093

---

## Настройка Kibana после установки

### 1. Создать Data View

Stack Management → Data Views → **Create data view**:

| Поле | Значение |
|------|---------|
| Name | mybank-logs |
| Index pattern | `monitoring-logs-*` |
| Timestamp field | `@timestamp` |

### 2. Настроить колонки в Discover

Analytics → Discover → добавить колонки:

- `service_name`
- `log.level`
- `message`
- `kubernetes.pod.name`

Сохранить как: **mybank-logs**

### 3. Полезные KQL-запросы

```
# Только ошибки и предупреждения
log.level: "ERROR" or log.level: "WARN"

# Логи конкретного сервиса
service_name: "accounts-service"

# Исключения
error.type: *

# Бизнес-события
tags: "business_event"

# Трассировка конкретного запроса
trace.id: "abc123"

# Ошибки за последний час
log.level: "ERROR" and @timestamp >= now-1h
```

---

## ECS JSON логирование

Все Spring Boot сервисы настроены на вывод логов в формате **Elastic Common Schema** — это стандартный формат Elastic, который Kibana понимает нативно.

Настройка в `configmap.yaml` каждого сервиса:

```yaml
logging:
  structured:
    format:
      console: ecs
```

Зависимости в `pom.xml` **не нужны** — ECS-формат встроен в Spring Boot 4 (3.4+).

Пример ECS-записи:

```json
{
  "@timestamp": "2026-03-07T10:00:00.000Z",
  "log.level": "INFO",
  "service.name": "accounts-service",
  "message": "User ID: 1 - Action: BALANCE_UPDATED",
  "trace.id": "abc123",
  "kubernetes.pod.name": "mybank-accounts-service-xyz"
}
```

---

## Logstash Pipeline

Logstash получает логи от Filebeat по beats-протоколу (порт 5044) и выполняет:

- Извлечение `service_name` из ECS-поля `service.name`
- Добавление `namespace` из метаданных Kubernetes
- Фильтрация шума (Kafka internal logs, ActiveMQ heartbeat)
- Парсинг бизнес-событий через grok: `User ID: X - Action: Y`
- Маршрутизация в Elasticsearch в индекс `monitoring-logs-YYYY.MM.dd`

---

## Обновление чарта

```bash
cd k8s/monitoring
helm upgrade k8s-monitoring . -n monitoring -f values-local.yaml
```

---

## Удаление

```bash
helm uninstall k8s-monitoring -n monitoring

# Удалить PVC Elasticsearch (данные логов)
kubectl delete pvc -n monitoring -l app=elasticsearch
```

---

## Диагностика

### Проверить что метрики собираются

```bash
# Все ServiceMonitor-ы
kubectl get servicemonitor -n mybank

# Состояние Prometheus targets
kubectl port-forward -n monitoring svc/k8s-monitoring-kube-prometheus-prometheus 9090:9090
# → http://localhost:9090/targets?search=mybank
```

### Проверить поток логов

```bash
# Filebeat отправляет данные?
kubectl logs -n monitoring -l app=filebeat --tail=50

# Logstash получает?
kubectl logs -n monitoring -l app=logstash --tail=50

# Elasticsearch принимает?
kubectl exec -n monitoring elasticsearch-0 -- \
  curl -s http://localhost:9200/_cat/indices?v | grep monitoring-logs
```

### Kibana не стартует (CrashLoopBackOff)

```bash
kubectl describe pod -n monitoring <kibana-pod>
```

Чаще всего причина — недостаточно памяти. Убедитесь что в `values.yaml` задано:

```yaml
kibana:
  resources:
    limits:
      memory: "1.5Gi"   # меньше 1Gi → OOMKilled
```

### Prometheus не видит mybank ServiceMonitor-ы

Убедитесь что в `values-local.yaml` заданы:

```yaml
kube-prometheus-stack:
  prometheus:
    prometheusSpec:
      serviceMonitorSelectorNilUsesHelmValues: false
      podMonitorSelectorNilUsesHelmValues: false
```

---

## Известные особенности

| Проблема | Статус | Комментарий |
|----------|--------|-------------|
| `node-exporter` CrashLoopBackOff | ⚠️ Известная | На docker-desktop проблема с `/sys/fs/cgroup`. Не влияет на остальное |
| Kibana медленный старт | ℹ️ Норма | Первый запуск занимает ~2 минуты |
| Zipkin Connection reset при старте | ℹ️ Норма | Zipkin стартует до ES, само проходит через 1-2 мин |
| `trace.id` не попадает в логи | 📋 Tech Debt | Требует настройки Micrometer Tracing + Zipkin bridge |

---

## values.yaml — ключевые параметры

```yaml
# Включение kube-prometheus-stack
prometheusStack:
  enabled: true

kube-prometheus-stack:
  prometheus:
    prometheusSpec:
      serviceMonitorSelectorNilUsesHelmValues: false  # важно!
  grafana:
    adminPassword: admin

elasticsearch:
  enabled: true
  persistence:
    size: 10Gi
  javaOpts: "-Xms512m -Xmx512m"

kibana:
  enabled: true
  resources:
    limits:
      memory: "1.5Gi"  # меньше → OOMKilled

filebeat:
  enabled: true
  watchNamespace: "mybank"  # собирать логи только из mybank
```

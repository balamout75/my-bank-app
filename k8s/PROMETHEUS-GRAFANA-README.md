# Мониторинг: Prometheus + Grafana

## Стек

| Компонент | Версия | Namespace | Порт |
|-----------|--------|-----------|------|
| kube-prometheus-stack | latest | `monitoring` | — |
| Prometheus | встроен | `monitoring` | 9090 |
| Grafana | встроен | `monitoring` | 3000 |
| Alertmanager | встроен | `monitoring` | 9093 |
| Node Exporter | встроен | `monitoring` | 9100 |
| kube-state-metrics | встроен | `monitoring` | 8080 |

---

## Часть 1 — Установка

### 1.1 Добавить Helm репозиторий

```bash
helm repo add prometheus-community https://prometheus-community.github.io/helm-charts
helm repo update
```

### 1.2 Установить kube-prometheus-stack

```bash
helm install monitoring prometheus-community/kube-prometheus-stack \
  --namespace monitoring \
  --create-namespace
```

### 1.3 Разрешить Prometheus Operator видеть ServiceMonitor-ы из других namespace

```bash
helm upgrade monitoring prometheus-community/kube-prometheus-stack \
  --namespace monitoring \
  --set prometheus.prometheusSpec.serviceMonitorSelectorNilUsesHelmValues=false \
  --set prometheus.prometheusSpec.podMonitorSelectorNilUsesHelmValues=false \
  --reuse-values
```

> **Зачем:** по умолчанию Operator ищет ServiceMonitor-ы только с лейблом
> `release: monitoring`. Этот флаг снимает ограничение — Prometheus
> подхватывает все ServiceMonitor-ы в любом namespace.

---

## Часть 2 — Изменения в Spring Boot сервисах

### 2.1 Добавить зависимость в pom.xml (каждый сервис)

```xml
<!-- Экспорт метрик в формате Prometheus -->
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
    <scope>runtime</scope>
</dependency>
```

> Версия управляется через Spring Boot BOM — указывать не нужно.

### 2.2 Actuator уже настроен в configmap

В `configmap.yaml` каждого сервиса уже есть:

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  endpoint:
    health:
      show-details: always
      probes:
        enabled: true
```

### 2.3 Исправить Security — открыть /actuator для Prometheus

**Проблема:** `oauth2ResourceServer` перехватывает запросы Prometheus
(без токена) раньше чем срабатывает `permitAll()` → 401 Unauthorized.

**Решение** в `SecurityConfig.java` каждого сервиса (accounts, cash, transfer):

```java
// Вместо .requestMatchers("/actuator/**").permitAll()
// использовать EndpointRequest:

.requestMatchers(EndpointRequest.toAnyEndpoint()).permitAll()
```

> `EndpointRequest.toAnyEndpoint()` — типобезопасный матчер Spring Boot,
> корректно работает с фильтрами OAuth2 Resource Server в Spring Boot 4.

**gateway-service** — отдельная конфигурация не нужна, там нет `oauth2ResourceServer`.

---

## Часть 3 — Helm чарт (папка k8s/)

### 3.1 Структура добавленных файлов

```
k8s/
├── templates/
│   └── prometheusrule.yaml          ← алерты (PrometheusRule CRD)
├── charts/
│   ├── accounts-service/
│   │   ├── templates/
│   │   │   └── servicemonitor.yaml  ← регистрация scrape target
│   │   └── values.yaml              ← добавлен блок monitoring.enabled
│   ├── cash-service/         (то же самое)
│   ├── transfer-service/     (то же самое)
│   ├── notifications-service/(то же самое)
│   └── gateway-service/      (то же самое, порт 8090)
└── values.yaml                      ← добавлен блок prometheusRules
```

### 3.2 ServiceMonitor — как работает

```yaml
# charts/accounts-service/templates/servicemonitor.yaml
apiVersion: monitoring.coreos.com/v1
kind: ServiceMonitor
metadata:
  name: {{ .Release.Name }}-accounts-service
  namespace: monitoring        # namespace Prometheus Operator-а
  labels:
    release: monitoring        # лейбл для Operator
spec:
  namespaceSelector:
    matchNames:
      - {{ .Release.Namespace }}   # namespace приложения (mybank)
  selector:
    matchLabels:
      app: accounts-service
  endpoints:
    - port: http
      path: /actuator/prometheus
      interval: 15s
```

### 3.3 PrometheusRule — как работает

Алерты хранятся в `values.yaml` под ключом `prometheusRules.groups`.
Шаблон `prometheusrule.yaml` передаёт их через `toYaml` без изменений:

```yaml
# templates/prometheusrule.yaml
spec:
  groups:
    {{- toYaml .Values.prometheusRules.groups | nindent 4 }}
```

> **Почему так:** Helm использует `{{ }}` как свои разделители.
> Если писать алерты прямо в шаблоне — Prometheus-переменные
> `{{ $labels.job }}` вызывают ошибку парсинга.
> Хранение в `values.yaml` + `toYaml` — официальный паттерн.

### 3.4 Деплой

```bash
cd k8s/

# Первый деплой
helm install mybank . --namespace mybank --create-namespace -f values-local.yaml

# Обновление
helm upgrade --install mybank . --namespace mybank -f values-local.yaml
```

---

## Часть 4 — Проверка установки

### 4.1 Проверить что ресурсы созданы

```bash
# ServiceMonitor-ы (должно быть 5 штук mybank-*)
kubectl get servicemonitor -n monitoring | grep mybank

# PrometheusRule (должен быть mybank-bank-alerts)
kubectl get prometheusrule -n monitoring | grep mybank

# Поды приложения
kubectl get pods -n mybank
```

### 4.2 Открыть интерфейсы

```bash
# Prometheus
kubectl port-forward -n monitoring svc/monitoring-kube-prometheus-prometheus 9090:9090

# Grafana
kubectl port-forward -n monitoring svc/monitoring-grafana 3000:80

# Alertmanager
kubectl port-forward -n monitoring svc/monitoring-alertmanager 9093:9093
```

### 4.3 Проверить targets в Prometheus

```
http://localhost:9090/targets?search=mybank
```

Все 5 сервисов должны быть **UP**:
- `mybank-accounts-service`
- `mybank-cash-service`
- `mybank-transfer-service`
- `mybank-notifications-service`
- `mybank-gateway-service`

### 4.4 Получить пароль Grafana

```bash
# Git Bash / Linux
kubectl get secret -n monitoring monitoring-grafana \
  -o jsonpath="{.data.admin-password}" | base64 --decode

# PowerShell
kubectl get secret -n monitoring monitoring-grafana `
  -o jsonpath="{.data.admin-password}" | `
  % { [System.Text.Encoding]::UTF8.GetString([System.Convert]::FromBase64String($_)) }
```

---

## Часть 5 — Grafana: дашборды

### 5.1 Импорт дашбордов

**Dashboards → New → Import → ввести ID → Load → выбрать datasource Prometheus**

| ID | Название | Что показывает |
|----|----------|----------------|
| `19004` | Spring Boot 3.x Statistics ✅ | HTTP метрики, JVM, latency |
| `1860` | Node Exporter Full | CPU, RAM, диск хоста |

> Дашборд `4701` (Spring Boot 2.1) — **не использовать**, устарел.
> Дашборд `17175` — требует Loki (Этап 2).

### 5.2 Как пользоваться дашбордом 19004

1. Открыть **Dashboards → 19004 Spring Boot Statistics**
2. Вверху выбрать:
   - **datasource** → `Prometheus`
   - **namespace** → `mybank`
   - **pod** → нужный сервис
3. Доступные метрики:
   - **Request rate** — запросы в секунду
   - **Error rate** — процент 5xx ошибок
   - **Response time** — latency P50/P95/P99
   - **JVM Heap** — использование памяти
   - **GC pauses** — паузы сборщика мусора
   - **Threads** — активные потоки

### 5.3 Kubernetes дашборды (встроенные)

**Dashboards → Kubernetes / Compute Resources / Namespace (Pods)**

- Вверху выбрать `namespace = mybank`
- Показывает CPU и RAM по каждому поду

---

## Часть 6 — Алерты

### 6.1 Просмотр алертов

```
http://localhost:9090/alerts
```

Или в Grafana: **Alerting → Alert rules → поиск "mybank"**

### 6.2 Настроенные алерты

| Алерт | Условие | Severity |
|-------|---------|----------|
| `ServiceDown` | Сервис недоступен > 1 мин | critical |
| `HighErrorRate` | 5xx > 5% запросов | critical |
| `SlowResponses` | P95 latency > 2с | warning |
| `HighHeapUsage` | JVM heap > 85% | warning |
| `PodCrashLooping` | Pod перезапускается | warning |
| `PodNotReady` | Pod не Ready > 5 мин | warning |

### 6.3 Статусы алертов

| Статус | Значение |
|--------|----------|
| **Inactive** | Условие не выполняется, всё хорошо |
| **Pending** | Условие выполняется, ждёт истечения `for:` |
| **Firing** | Алерт сработал |

---

## Часть 7 — Troubleshooting

### Targets DOWN с ошибкой 401

Spring Security блокирует Prometheus. Решение — использовать
`EndpointRequest.toAnyEndpoint()` в `SecurityConfig`:

```java
.requestMatchers(EndpointRequest.toAnyEndpoint()).permitAll()
```

### ServiceMonitor не появляется в Prometheus

Prometheus Operator не видит ServiceMonitor из другого namespace.
Выполнить команду из п. 1.3.

### helm upgrade — ошибка "Rules are not valid"

Webhook Prometheus Operator отклоняет невалидный PromQL.
Проверить выражения в `values.yaml` секция `prometheusRules.groups`.

### Дашборд в Grafana пустой

Вверху дашборда проверить дропдауны — выбрать `namespace=mybank`
и нужный `pod`.

---

## Следующий шаг

**Этап 2 — ELK:** централизованный сбор и анализ логов через
Elasticsearch + Logstash + Kibana + Filebeat.

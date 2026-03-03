# MyBank K8s — Справочник команд

## 🚀 Полный деплой с нуля

```bash
# 1. CoreDNS rewrite (keycloak внутри кластера)
kubectl edit configmap coredns -n kube-system
# Добавить перед "kubernetes cluster.local":
#   rewrite name keycloak.mybank.local mybank-keycloak.mybank.svc.cluster.local
kubectl rollout restart deployment coredns -n kube-system

# 2. Ingress-nginx
kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/controller-v1.12.0/deploy/static/provider/cloud/deploy.yaml
kubectl get pods -n ingress-nginx -w  # ждать 1/1 Running

# 3. Деплой MyBank
cd C:\Practicum\module_three\Ten\my-bank-app\k8s
helm dependency update .
helm install mybank . --namespace mybank --create-namespace
kubectl get pods -n mybank -w  # ждать все 1/1 Running
```

## 🔄 Обновление

```bash
# Обновить Helm-релиз после изменений в чартах
helm upgrade mybank . --namespace mybank

# Рестарт одного сервиса
kubectl rollout restart deployment mybank-transfer-service -n mybank

# Рестарт всех сервисов
kubectl rollout restart deployment -n mybank mybank-accounts-service mybank-cash-service mybank-transfer-service mybank-notifications-service mybank-gateway-service mybank-front-ui

# Рестарт после пересборки образов
docker buildx bake --load -f docker-bake.hcl
kubectl rollout restart deployment -n mybank mybank-accounts-service mybank-cash-service mybank-transfer-service mybank-notifications-service mybank-gateway-service mybank-front-ui
```

## 🔨 Сборка Docker-образов

```bash
# Все сервисы
docker buildx bake --load -f docker-bake.hcl

# Один сервис
docker buildx bake --load -f docker-bake.hcl transfer-service

# Проверить образы
docker images | findstr mybank
```

## 📋 Мониторинг

```bash
# Статус подов
kubectl get pods -n mybank
kubectl get pods -n mybank -w          # в реальном времени

# Логи сервиса
kubectl logs -n mybank -l app=transfer-service --tail=50
kubectl logs -n mybank -l app=accounts-service --tail=50
kubectl logs -n mybank -l app=cash-service --tail=50
kubectl logs -n mybank -l app=notifications-service --tail=50
kubectl logs -n mybank -l app=gateway-service --tail=50
kubectl logs -n mybank -l app=front-ui --tail=50
kubectl logs -n mybank -l app=keycloak --tail=50

# Логи конкретного пода
kubectl logs -n mybank <pod-name> --tail=50

# Follow логов (в реальном времени)
kubectl logs -n mybank -l app=transfer-service -f

# Все ресурсы
kubectl get all -n mybank

# Сервисы и их порты
kubectl get svc -n mybank

# Ingress
kubectl get ingress -n mybank

# ConfigMap
kubectl get configmap -n mybank
kubectl get configmap mybank-accounts-service -n mybank -o yaml

# Secrets
kubectl get secrets -n mybank
```

## 🔍 Отладка

```bash
# Описание пода (события, причины ошибок)
kubectl describe pod -n mybank <pod-name>

# Exec в под (shell)
kubectl exec -it -n mybank <pod-name> -- /bin/sh

# Проверка DNS изнутри пода
kubectl exec -n mybank <pod-name> -- nslookup keycloak.mybank.local
kubectl exec -n mybank <pod-name> -- nslookup mybank-accounts-service.mybank.svc.cluster.local

# Проверка HTTP изнутри пода
kubectl exec -n mybank <pod-name> -- curl -s http://mybank-accounts-service:8080/actuator/health
kubectl exec -n mybank <pod-name> -- curl -s http://mybank-keycloak:8080/realms/mybank

# CoreDNS проверка
kubectl get configmap coredns -n kube-system -o yaml | findstr rewrite
kubectl get pods -n kube-system | findstr coredns

# Ingress-nginx проверка
kubectl get pods -n ingress-nginx
kubectl get svc -n ingress-nginx
```

## 🗑️ Удаление

```bash
# Удалить MyBank
helm uninstall mybank -n mybank
kubectl delete namespace mybank

# Удалить Ingress-nginx
kubectl delete -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/controller-v1.12.0/deploy/static/provider/cloud/deploy.yaml

# Полный сброс Kubernetes
# Docker Desktop → Settings → Kubernetes → Reset Kubernetes Cluster
```

## 📁 Структура проекта

```
my-bank-app/k8s/
├── Chart.yaml              # Зонтичный чарт
├── values.yaml             # Глобальные настройки
└── charts/
    ├── postgresql/          # БД
    ├── keycloak/            # OAuth2 сервер
    ├── gateway-service/     # API Gateway (порт 8090)
    ├── front-ui/            # UI (порт 8081)
    ├── accounts-service/    # Счета (порт 8080)
    ├── cash-service/        # Наличные (порт 8080)
    ├── transfer-service/    # Переводы (порт 8080)
    └── notifications-service/ # Уведомления (порт 8080)
```

## 🌐 URLs

| Сервис | URL |
|--------|-----|
| Frontend | http://mybank.local |
| Keycloak Admin | http://keycloak.mybank.local/admin/ |
| Keycloak Login | admin / admin |

## 🔑 Hosts файл (C:\Windows\System32\drivers\etc\hosts)

```
127.0.0.1 mybank.local keycloak.mybank.local
```

## ⚠️ Частые проблемы

| Проблема | Решение |
|----------|---------|
| `No servers available for service` | Добавить `spring.cloud.discovery.client.simple.instances` в ConfigMap |
| `Connection refused keycloak.mybank.local` | Проверить CoreDNS rewrite: `kubectl get configmap coredns -n kube-system -o yaml` |
| `CrashLoopBackOff` | Проверить логи: `kubectl logs -n mybank <pod> --tail=50` |
| `403 Forbidden` | Проверить SecurityConfig (правильный client-id в `resourceAccess.get()`) и роли в Keycloak |
| Образ не обновился | `docker buildx bake --load` + `kubectl rollout restart deployment` |
| HSTS redirect loop | Убедиться что Ingress использует http, не https |
| Прокси перехватывает запросы | Добавить `mybank.local;keycloak.mybank.local` в исключения прокси Windows |

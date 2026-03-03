# MyBank — Технический долг

## Инфраструктура

- [ ] **CoreDNS rewrite** — заменить на `jwk-set-uri` + `token-uri` в ConfigMap, убрать зависимость от CoreDNS
- [ ] **Ingress-nginx** — мигрировать на Gateway API (Kubernetes стандарт)
- [ ] **Init Containers** — добавить wait-for-keycloak, wait-for-postgresql вместо CrashLoopBackOff
- [ ] **imagePullSecrets** — настроить для pull из GHCR в K8s (сейчас работает через local images)
- [ ] **Base images в GHCR** — закешировать `eclipse-temurin:21-jre-alpine` и `maven:3.9-eclipse-temurin-21-alpine` в GHCR, убрать зависимость от Docker Hub

## Секреты

- [ ] **Keycloak realm secrets** — вынести client secrets из realm-mybank.json в Helm values / Jenkins credentials
- [x] **values.yaml** — убрать default секреты (clientSecret) из values.yaml подчартов
- [ ] **Sealed Secrets / External Secrets** — внедрить для production

## CI/CD

- [ ] **Mail notifications** — настроить SMTP в Jenkins для отправки результатов сборки
- [ ] **Параллельные тесты** — разделить `mvn clean install` на параллельные stage по сервисам
- [ ] **Webhook trigger** — настроить GitHub webhook вместо ручного Scan Repository
- [ ] **GitHub commit status** — добавить `repo:status` scope в токен для обновления статуса коммита
- [ ] **Git tags** — автоматически тегировать релизы (v1, v2...) после успешного PROD-деплоя
- [x] **Двойная сборка Maven** — устранена, один `mvn clean install` вместо двух проходов
- [x] **Docker build + push** — параллельная сборка и push всех 6 образов
- [x] **Dockerfile.ci** — легковесный Dockerfile для Jenkins (копирует готовый JAR, без Maven внутри)
- [x] **Secrets management** — секреты вынесены из values.yaml, передаются через `--set` / `values-local.yaml`

## Код

- [x] **Config-service / Discovery-service** — удалены из parent pom.xml и docker-bake.hcl (не нужны в K8s)
- [ ] **Spring profiles** — унифицировать конфигурацию docker/k8s профилей
- [ ] **Health checks** — добавить readiness/liveness зависимости от Keycloak и PostgreSQL
- [ ] **openAPI** — добавить спецификации для всех REST-эндпоинтов
- [ ] **Обработка исключений** — стандартизировать error response, улучшить диагностику

## Документация

- [ ] **README** — добавить диаграмму CI/CD pipeline
- [x] **.env.example** — заменить реальные секреты на плейсхолдеры во всех примерах
- [x] **README** — обновлён: убраны Docker Compose / Eureka / Config Server, K8s-only
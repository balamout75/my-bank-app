# MyBank — Технический долг

## Инфраструктура

- [ ] **CoreDNS rewrite** — заменить на `jwk-set-uri` + `token-uri` в ConfigMap, убрать зависимость от CoreDNS
- [ ] **Ingress-nginx** — мигрировать на Gateway API (Kubernetes стандарт)
- [ ] **Init Containers** — добавить wait-for-keycloak, wait-for-postgresql вместо CrashLoopBackOff
- [ ] **imagePullSecrets** — настроить для pull из GHCR в K8s (сейчас работает через local images)

## Секреты

- [ ] **Keycloak realm secrets** — вынести client secrets из realm-mybank.json в Helm values / Jenkins credentials
- [ ] **values.yaml** — убрать default секреты (clientSecret) из values.yaml подчартов
- [ ] **Sealed Secrets / External Secrets** — внедрить для production

## CI/CD

- [ ] **Mail notifications** — настроить SMTP в Jenkins для отправки результатов сборки
- [ ] **Параллельные тесты** — разделить `mvn clean install` на параллельные stage по сервисам
- [ ] **Docker layer caching** — оптимизировать Dockerfile.build для Jenkins (сейчас пересборка медленная)
- [ ] **Webhook trigger** — настроить GitHub webhook вместо ручного Scan Repository
- [ ] **GitHub commit status** — добавить permission в токен для обновления статуса коммита

## Код

- [ ] **Config-service / Discovery-service** — убрать из docker-bake.hcl группы default (не нужны в K8s)
- [ ] **Spring profiles** — унифицировать конфигурацию docker/k8s профилей
- [ ] **Health checks** — добавить readiness/liveness зависимости от Keycloak и PostgreSQL

## Документация

- [ ] **README** — добавить диаграмму CI/CD pipeline
- [ ] **.env.example** — заменить реальные секреты на плейсхолдеры во всех примерах

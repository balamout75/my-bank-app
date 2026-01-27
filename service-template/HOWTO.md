# 🎯 Быстрая инструкция: Как создать новый микросервис

## Для нетерпеливых 😎

```bash
# 1. Скопировать шаблон
cp -r service-template accounts-service
cd accounts-service

# 2. Глобальная замена в IDE:
#    template → accounts
#    Template → Accounts
#    TEMPLATE → ACCOUNTS

# 3. Переименовать файлы:
#    ServiceTemplateApplication.java → AccountsServiceApplication.java
#    TemplateEntity.java → Account.java
#    TemplateRepository.java → AccountRepository.java
#    TemplateService.java → AccountService.java
#    TemplateController.java → AccountController.java

# 4. Обновить pom.xml:
<artifactId>accounts-service</artifactId>

# 5. Обновить application.yml:
server:
  port: 8081
spring:
  application:
    name: accounts-service
  datasource:
    url: jdbc:postgresql://localhost:5432/mybank?currentSchema=accounts

# 6. Добавить в корневой pom.xml:
<module>accounts-service</module>

# 7. Собрать и запустить:
cd ..
mvn clean install
mvn spring-boot:run -pl accounts-service
```

## Детально

### Что менять ОБЯЗАТЕЛЬНО:

| Файл | Что менять | Пример |
|------|------------|--------|
| **pom.xml** | `<artifactId>` | `accounts-service` |
| **application.yml** | `server.port` | `8081` |
| **application.yml** | `spring.application.name` | `accounts-service` |
| **application.yml** | `datasource.url` (схема) | `currentSchema=accounts` |
| **application.yml** | `logging` (пакет) | `com.mybank.accounts` |
| **Entity** | `@Table(name)` | `accounts` |
| **Controller** | `@RequestMapping` | `/api/accounts` |
| **Все классы** | имена классов | `Account`, `AccountService`, ... |
| **Все пакеты** | пакеты | `com.mybank.accounts` |

### Что менять ОПЦИОНАЛЬНО:

- Раскомментировать Security (если нужна защита OAuth2)
- Раскомментировать Feign Client (если нужно вызывать другие сервисы)
- Раскомментировать Circuit Breaker (для отказоустойчивости)
- Раскомментировать Testcontainers (для интеграционных тестов)

### Порты сервисов:

```
8761 - Discovery Service
8888 - Config Service
9000 - OAuth2 Server
8090 - Gateway Service
8080 - Front UI
8081 - Accounts Service     ← новый сервис
8082 - Cash Service         ← новый сервис
8083 - Transfer Service     ← новый сервис
8084 - Notifications Service ← новый сервис
```

### Проверка:

```bash
# 1. Компиляция
mvn clean compile

# 2. Тесты
mvn test

# 3. Запуск
mvn spring-boot:run

# 4. Health check
curl http://localhost:8081/actuator/health

# 5. Eureka Dashboard
open http://localhost:8761
# Должен появиться ваш сервис!
```

## Типичные ошибки

❌ **Забыли переименовать пакет** → Компиляция не проходит
✅ Используйте Find & Replace в IDE

❌ **Забыли изменить порт** → Конфликт портов
✅ Каждому сервису свой уникальный порт

❌ **Забыли изменить имя приложения** → Eureka показывает "service-template"
✅ Проверьте `spring.application.name` в application.yml

❌ **Забыли изменить схему БД** → Конфликт таблиц
✅ Каждому сервису своя схема PostgreSQL

❌ **Забыли добавить модуль в корневой pom.xml** → Модуль не собирается
✅ Добавьте `<module>your-service</module>`

---

**Готово! Можно кодить бизнес-логику!** 🚀

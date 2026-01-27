# My Bank Application

## Описание
Микросервисное банковское приложение на Spring Boot и Spring Cloud.

## Текущий статус: Шаг 1 - Корневой Maven проект

На данный момент создан корневой (parent) Maven проект с базовой конфигурацией.

### Что уже есть:

1. **pom.xml** - родительский POM с:
   - Java 21
   - Spring Boot 3.2.1
   - Spring Cloud 2023.0.0
   - Управление зависимостями (dependencyManagement)
   - Управление плагинами (pluginManagement)
   - Пустой секцией modules (будем добавлять модули постепенно)

2. **.gitignore** - игнорирование временных файлов, IDE, Maven target и т.д.

### Структура проекта:

```
my-bank-app/
├── pom.xml          # Родительский POM
├── .gitignore       # Git ignore файл
└── README.md        # Этот файл
```

### Следующие шаги:

Будем постепенно добавлять модули:
1. Discovery Service (Eureka)
2. Config Service
3. OAuth2 Server
4. Gateway Service
5. Accounts Service
6. Cash Service
7. Transfer Service
8. Notifications Service
9. Front UI

### Проверка работоспособности:

```bash
# Проверить что POM корректен
mvn validate

# Очистить проект
mvn clean
```

### Требования:

- JDK 21 или выше
- Maven 3.8+

---

**Проект готов к добавлению первого модуля!**

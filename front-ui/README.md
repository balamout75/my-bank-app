# Front UI - Веб-интерфейс банковского приложения

## Описание
Веб-интерфейс для взаимодействия пользователя с банковскими микросервисами.

## Технологии
- **Spring Boot 3.2.1**
- **Spring Web MVC** - для обработки HTTP запросов
- **Thymeleaf** - шаблонизатор для HTML
- **Spring Security** - безопасность
- **OAuth2 Client** - авторизация через Authorization Code Flow
- **Bootstrap 5** - CSS фреймворк

## Функциональность

### 1. Информация об аккаунте
- Просмотр данных: имя, фамилия, дата рождения, баланс
- Редактирование личных данных
- Валидация возраста (минимум 18 лет)

### 2. Операции с деньгами
- **Пополнение счета** - внесение денег на счет
- **Снятие денег** - снятие со счета (с проверкой баланса)

### 3. Переводы
- Перевод денег другому пользователю
- Выбор получателя из списка
- Проверка достаточности средств

## Структура модуля

```
front-ui/
├── src/
│   ├── main/
│   │   ├── java/com/mybank/frontend/
│   │   │   ├── FrontendApplication.java       # Главный класс
│   │   │   ├── controller/
│   │   │   │   └── MainController.java        # Основной контроллер
│   │   │   ├── dto/
│   │   │   │   └── FrontendDTO.java          # DTO для форм и данных
│   │   │   └── config/
│   │   │       ├── SecurityConfig.java        # OAuth2 Login
│   │   │       └── RestTemplateConfig.java    # HTTP клиент
│   │   └── resources/
│   │       ├── application.yml                # Конфигурация
│   │       ├── templates/
│   │       │   ├── main.html                  # Главная страница
│   │       │   └── error.html                 # Страница ошибки
│   │       └── static/
│   │           └── css/
│   │               └── style.css              # Стили
│   └── test/
│       └── java/com/mybank/frontend/
│           └── FrontendApplicationTests.java  # Тесты
└── pom.xml
```

## Как работает

### OAuth2 Authorization Code Flow

1. **Пользователь заходит на http://localhost:8080**
2. **Spring Security перенаправляет на OAuth2 Server** (http://localhost:9000)
3. **Пользователь вводит логин/пароль** (например, user1/password1)
4. **OAuth2 Server возвращает authorization code**
5. **Front UI обменивает code на Access Token (JWT)**
6. **Token автоматически добавляется к каждому запросу** через RestTemplate interceptor

### Взаимодействие с микросервисами

Все запросы идут через **Gateway API** (http://localhost:8090):

```
Front UI → Gateway → Accounts Service
        → Gateway → Cash Service
        → Gateway → Transfer Service
```

Gateway проверяет JWT токен и перенаправляет запросы к нужным сервисам.

## Конфигурация

### application.yml

```yaml
server:
  port: 8080

spring:
  security:
    oauth2:
      client:
        registration:
          front-client:
            client-id: front-client          # ID клиента
            client-secret: front-secret       # Секрет
            scope:
              - openid
              - accounts.read
              - accounts.write
              - cash.read
              - cash.write
              - transfer.read
              - transfer.write

gateway:
  url: http://localhost:8090  # URL Gateway API
```

## Endpoints

- `GET /` - главная страница (требует авторизации)
- `POST /account/update` - обновить данные аккаунта
- `POST /cash/deposit` - пополнить счет
- `POST /cash/withdraw` - снять деньги
- `POST /transfer` - перевести деньги
- `GET /logout` - выход из системы

## Зависимости от других сервисов

Для работы необходимы:
1. **OAuth2 Server** (порт 9000) - для авторизации
2. **Gateway Service** (порт 8090) - для маршрутизации запросов
3. **Accounts Service** (порт 8081) - для работы с аккаунтами
4. **Cash Service** (порт 8082) - для операций с деньгами
5. **Transfer Service** (порт 8083) - для переводов

## Запуск

### Локально:
```bash
cd front-ui
mvn spring-boot:run
```

### Из корня проекта:
```bash
mvn spring-boot:run -pl front-ui
```

### Доступ:
```
http://localhost:8080
```

### Тестовые пользователи (после настройки OAuth2 Server):
- user1 / password1
- user2 / password2
- admin / admin

## Особенности реализации

### 1. Автоматическая подстановка токена
`RestTemplateConfig` добавляет OAuth2 токен к каждому HTTP запросу автоматически.

### 2. Обработка ошибок
Все HTTP ошибки перехватываются и отображаются пользователю в виде alert сообщений.

### 3. Flash attributes
Используются для передачи сообщений об успехе/ошибке после redirect.

### 4. Валидация на клиенте и сервере
- HTML5 валидация (`required`, `min`, `step`)
- Bean Validation аннотации (@NotBlank, @Past, @DecimalMin)

## TODO

- [ ] Добавить историю операций
- [ ] Добавить пагинацию для списка аккаунтов
- [ ] Добавить подтверждение для критичных операций
- [ ] Добавить темную тему
- [ ] Улучшить адаптивность для мобильных устройств

---

**Модуль готов!** Можно запускать после создания других сервисов.

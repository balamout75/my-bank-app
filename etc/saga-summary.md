# SAGA Pattern — Summary для MyBank

## Текущая архитектура (без SAGA)

```
Frontend → Gateway → cash-service/transfer-service → accounts-service → notifications-service
```

### Что реализовано:
- **Transactional Outbox** — надёжная доставка событий между сервисами
- **State Machine** — отслеживание состояния операций (RESERVED → IN_PROGRESS → UPDATED → NOTIFIED)
- **Atomic Transaction** — transfer выполняется атомарно в accounts-service (одна БД)

### Почему SAGA не нужна сейчас:
- Transfer (снятие + зачисление) происходит **в одной транзакции** внутри accounts-service
- Нет распределённой транзакции между разными БД

---

## Когда нужна SAGA

Когда **одна бизнес-операция затрагивает несколько сервисов с разными БД**:

### Пример проблемы:
```
Перевод: Bank A (своя БД) → Bank B (своя БД)

Step 1: Снять у Alice (Bank A)     ✅ Успех
Step 2: Положить Bob (Bank B)      ❌ Failed!
Step 3: ??? Деньги пропали — Alice потеряла, Bob не получил
```

### Решение — SAGA:
Последовательность локальных транзакций с **компенсирующими операциями** для отката.

---

## Два подхода к SAGA

### 1. Choreography (хореография)

Сервисы общаются через события, нет центрального координатора.

```
┌────────┐  MoneyDebited  ┌────────┐  MoneyCredited  ┌────────┐
│ Bank A │───────────────▶│ Bank B │────────────────▶│ Notify │
└────────┘                └────────┘                 └────────┘
     │                         │
     │    DebitFailed          │    CreditFailed
     │◀────────────────────────│◀───────────────────
     │                         │
     ▼                         ▼
  Compensate               Compensate
  (возврат)                (возврат A)
```

**Плюсы:** Простота, децентрализация
**Минусы:** Сложно отслеживать, циклические зависимости

### 2. Orchestration (оркестрация)

Центральный оркестратор управляет шагами саги.

```
              ┌─────────────┐
              │ Orchestrator│
              └──────┬──────┘
                     │
         ┌───────────┼───────────┐
         ▼           ▼           ▼
    ┌────────┐  ┌────────┐  ┌────────┐
    │ Step 1 │  │ Step 2 │  │ Step 3 │
    │ Debit  │  │ Credit │  │ Notify │
    └────────┘  └────────┘  └────────┘
         │           │
         ▼           ▼
    Compensate   Compensate
    if failed    if failed
```

**Плюсы:** Легко отслеживать, понятный flow
**Минусы:** Single point of failure, оркестратор — бутылочное горлышко

---

## Пример реализации SAGA Orchestration

### Java код оркестратора:

```java
@Service
public class TransferSagaOrchestrator {

    public void executeTransfer(TransferRequest req) {
        String sagaId = UUID.randomUUID().toString();
        
        try {
            // Step 1: Снять деньги (но не подтверждать)
            bankAClient.debit(sagaId, req.from(), req.amount());  // → PENDING
            saveSagaStep(sagaId, "WITHDRAW", "COMPLETED");
            
            // Step 2: Зачислить деньги
            bankBClient.credit(sagaId, req.to(), req.amount());   // → PENDING
            saveSagaStep(sagaId, "DEPOSIT", "COMPLETED");
            
            // Step 3: Подтвердить обе операции
            bankAClient.confirm(sagaId);  // PENDING → COMPLETED
            bankBClient.confirm(sagaId);  // PENDING → COMPLETED
            
            saveSagaStatus(sagaId, "COMPLETED");
            
        } catch (Exception e) {
            // Compensate — откатить всё что успели сделать
            compensate(sagaId);
            saveSagaStatus(sagaId, "COMPENSATED");
            throw new TransferFailedException(e);
        }
    }
    
    private void compensate(String sagaId) {
        List<SagaStep> steps = getSagaSteps(sagaId);
        
        // Откатываем в обратном порядке
        for (SagaStep step : reversed(steps)) {
            if (step.status == COMPLETED) {
                switch (step.name) {
                    case "WITHDRAW" -> bankAClient.compensate(sagaId);  // возврат
                    case "DEPOSIT" -> bankBClient.compensate(sagaId);   // отмена
                }
            }
        }
    }
}
```

### Структура журнала саги:

```sql
CREATE TABLE transfer_saga (
    saga_id       UUID PRIMARY KEY,
    operation_id  BIGINT NOT NULL,
    from_user     VARCHAR(128) NOT NULL,
    to_user       VARCHAR(128) NOT NULL,
    amount        NUMERIC(19,2) NOT NULL,
    status        VARCHAR(20) NOT NULL,  -- STARTED, COMPLETED, COMPENSATING, COMPENSATED, FAILED
    created_at    TIMESTAMP DEFAULT NOW(),
    completed_at  TIMESTAMP
);

CREATE TABLE transfer_saga_steps (
    id            BIGSERIAL PRIMARY KEY,
    saga_id       UUID NOT NULL REFERENCES transfer_saga(saga_id),
    step_name     VARCHAR(20) NOT NULL,  -- WITHDRAW, DEPOSIT, NOTIFY
    step_order    INT NOT NULL,
    status        VARCHAR(20) NOT NULL,  -- PENDING, COMPLETED, COMPENSATED, FAILED
    service       VARCHAR(50),           -- bank-a-service, bank-b-service
    request       JSONB,                 -- запрос
    response      JSONB,                 -- ответ
    error         TEXT,
    created_at    TIMESTAMP DEFAULT NOW(),
    completed_at  TIMESTAMP
);

-- Индексы
CREATE INDEX idx_saga_status ON transfer_saga(status);
CREATE INDEX idx_saga_steps_saga_id ON transfer_saga_steps(saga_id);
```

---

## Сравнение подходов

| Критерий | Atomic TX | Outbox | SAGA |
|----------|-----------|--------|------|
| Сложность | Низкая | Средняя | Высокая |
| Когда использовать | Одна БД | Надёжная доставка событий | Несколько БД/сервисов |
| Консистентность | Strong | Eventual | Eventual |
| Откат | Автоматический (ROLLBACK) | Нет отката | Компенсирующие операции |
| Пример | accounts-service.transfer() | cash → notifications | Bank A → Bank B |

---

## Применение к MyBank

### Текущее состояние (достаточно):
```
transfer-service → accounts-service.transfer(from, to, amount)
                         │
                         ▼
                   Одна транзакция в PostgreSQL:
                   UPDATE accounts SET balance = balance - 1000 WHERE user = 'alice';
                   UPDATE accounts SET balance = balance + 1000 WHERE user = 'bob';
                   COMMIT; -- атомарно
```

### Когда переходить на SAGA:
1. Если разнести балансы пользователей в **разные микросервисы** (alice-account-service, bob-account-service)
2. Если добавить **внешние платёжные системы** (Stripe, банковские API)
3. Если нужен **hold/reserve** денег перед подтверждением
4. Если требуется **детальный аудит** каждого шага операции

---

## Ресурсы для изучения

- [Microservices Patterns by Chris Richardson](https://microservices.io/patterns/data/saga.html)
- [Eventuate Tram SAGA Framework](https://eventuate.io/docs/manual/eventuate-tram/latest/getting-started-eventuate-tram-sagas.html)
- [Axon Framework (Java)](https://docs.axoniq.io/reference-guide/axon-framework/sagas)
- [Spring State Machine](https://docs.spring.io/spring-statemachine/docs/current/reference/)

---

## TODO для следующей сессии

- [ ] Разделить accounts-service на отдельные сервисы по пользователям (для демонстрации)
- [ ] Реализовать SAGA Orchestrator для transfer
- [ ] Добавить компенсирующие операции
- [ ] Добавить таблицы saga/saga_steps
- [ ] Реализовать retry с экспоненциальным backoff для шагов саги
- [ ] Добавить мониторинг состояния саг (актуатор/метрики)

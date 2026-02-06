package com.mybank.cash.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record CashOperationRequest(
        @NotNull(message = "Operation ID обязателен")
        Long operationId,
        @NotNull(message = "Тип операции обязателен")
        CashOperationType cashOperationType,
        @NotNull(message = "Сумма обязательна")
        @DecimalMin(value = "0.01", message = "Сумма должна быть больше 0")
        BigDecimal amount
) {}

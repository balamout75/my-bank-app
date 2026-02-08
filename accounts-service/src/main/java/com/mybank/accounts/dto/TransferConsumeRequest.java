package com.mybank.accounts.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record TransferConsumeRequest(
        @NotNull(message = "Operation ID обязателен")
        Long operationId,

        @NotBlank(message = "Отправитель обязателен")
        String username,

        @NotBlank(message = "Получатель обязателен")
        String recipient,

        @NotNull(message = "Сумма обязательна")
        @DecimalMin(value = "0.01", message = "Сумма должна быть не менее 0.01")
        BigDecimal amount
) {}
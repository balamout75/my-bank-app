package com.mybank.transfer.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record TransferOperationRequest(
        @NotNull(message = "Operation ID обязателен")
        Long operationId,
        @NotNull(message = "ПОлучатель обязателен")
        String recipient,
        @NotNull(message = "Сумма обязательна")
        @DecimalMin(value = "0.01", message = "Сумма должна быть больше 0")
        BigDecimal amount

) {}

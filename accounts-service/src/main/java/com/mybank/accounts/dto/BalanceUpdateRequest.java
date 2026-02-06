package com.mybank.accounts.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record BalanceUpdateRequest(
        @NotNull String username,
        @NotNull @DecimalMin("0.01") BigDecimal amount,
        @NotNull CashOperationType cashOperationType,
        @NotNull Long operationId
) { }

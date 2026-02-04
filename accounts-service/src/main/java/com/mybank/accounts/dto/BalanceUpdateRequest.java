package com.mybank.accounts.dto;

import com.mybank.cash.dto.CashOperationType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record BalanceUpdateRequest(
        @NotNull String username,
        @NotNull @DecimalMin("0.01") BigDecimal amount,
        @NotNull CashOperationType operationType,
        @NotNull Long operationId
) { }

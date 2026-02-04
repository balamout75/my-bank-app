package com.mybank.cash.dto;

import java.math.BigDecimal;

public record BalanceUpdateRequest(
        String username,
        BigDecimal amount,
        OperationType operationType,  // "DEPOSIT" или "WITHDRAW"
        Long operationId
) {}
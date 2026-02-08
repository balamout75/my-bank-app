package com.mybank.transfer.dto;

import java.math.BigDecimal;

public record BalanceUpdateRequest(
        String username,
        BigDecimal amount,
        CashOperationType cashOperationType,  // "DEPOSIT" или "WITHDRAW"
        Long operationId
) {}
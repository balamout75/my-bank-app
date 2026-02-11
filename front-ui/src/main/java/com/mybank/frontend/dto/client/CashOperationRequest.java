package com.mybank.frontend.dto.client;

import java.math.BigDecimal;

public record CashOperationRequest(
        Long operationId,
        CashOperationType cashOperationType,
        BigDecimal amount
) {}

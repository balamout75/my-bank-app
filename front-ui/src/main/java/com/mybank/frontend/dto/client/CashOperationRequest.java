package com.mybank.frontend.dto.client;

import java.math.BigDecimal;

public record CashOperationRequest(
        Long operationId,
        OperationType operationType,
        BigDecimal amount
) {}

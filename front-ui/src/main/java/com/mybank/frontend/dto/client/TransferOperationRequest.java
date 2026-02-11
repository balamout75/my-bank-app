package com.mybank.frontend.dto.client;

import java.math.BigDecimal;

public record TransferOperationRequest(
        Long operationId,
        String recipient,
        BigDecimal amount
) {}

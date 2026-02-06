package com.mybank.frontend.dto.client;

import java.math.BigDecimal;

public record TransferOperationRequest(
        Long operationId,
        String username,
        BigDecimal amount
) {}

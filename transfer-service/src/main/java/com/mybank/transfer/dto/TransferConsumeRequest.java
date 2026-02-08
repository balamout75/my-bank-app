package com.mybank.transfer.dto;

import java.math.BigDecimal;

public record TransferConsumeRequest(
        Long operationId,
        String username,
        String recipient,
        BigDecimal amount
) {}
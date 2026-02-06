package com.mybank.frontend.exception;

import java.math.BigDecimal;

public class InsufficientFundsClientException extends RuntimeException {
    private final BigDecimal currentBalance;
    private final BigDecimal requestedAmount;

    public InsufficientFundsClientException(String message, BigDecimal currentBalance, BigDecimal requestedAmount) {
        super(message);
        this.currentBalance = currentBalance;
        this.requestedAmount = requestedAmount;
    }

    public BigDecimal getCurrentBalance() { return currentBalance; }
    public BigDecimal getRequestedAmount() { return requestedAmount; }
}



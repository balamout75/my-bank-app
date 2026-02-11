package com.mybank.template.exception;

import java.math.BigDecimal;

/**
 * Исключение недостаточного баланса
 * HTTP Status: 400 BAD REQUEST
 */
public class InsufficientBalanceException extends BusinessException {
    
    public InsufficientBalanceException(BigDecimal required, BigDecimal available) {
        super(String.format("Недостаточно средств. Требуется: %s, Доступно: %s", 
                           required, available),
              "INSUFFICIENT_BALANCE");
    }
    
    public InsufficientBalanceException(String message) {
        super(message, "INSUFFICIENT_BALANCE");
    }
}

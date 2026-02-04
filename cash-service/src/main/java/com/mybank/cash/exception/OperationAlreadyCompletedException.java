package com.mybank.cash.exception;

public class OperationAlreadyCompletedException extends RuntimeException {
    public OperationAlreadyCompletedException(String message) {
        super(message);
    }
}
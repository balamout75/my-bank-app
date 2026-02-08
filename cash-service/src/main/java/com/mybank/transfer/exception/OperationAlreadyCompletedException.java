package com.mybank.transfer.exception;

public class OperationAlreadyCompletedException extends RuntimeException {
    public OperationAlreadyCompletedException(String message) {
        super(message);
    }
}
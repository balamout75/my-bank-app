package com.mybank.transfer.exception;

public class InvalidOperationKeyException extends RuntimeException {

    public InvalidOperationKeyException(String message) {
        super(message);
    }

    public InvalidOperationKeyException(String message, Throwable cause) {
        super(message, cause);
    }
}
package com.mybank.accounts.exception;

public class AccountNotFoundException extends RuntimeException {

    public AccountNotFoundException(String username) {
        super("Аккаунт не найден: " + username);
    }
}
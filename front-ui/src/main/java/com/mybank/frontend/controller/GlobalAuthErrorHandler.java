package com.mybank.frontend.controller;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.client.HttpClientErrorException;

@ControllerAdvice
public class GlobalAuthErrorHandler {

    @ExceptionHandler(HttpClientErrorException.Unauthorized.class)
    public String handleUnauthorized() {
        // токен/сессия клиента невалидны -> сбросить локальную сессию + OIDC logout
        return "redirect:/force-logout";
    }
}

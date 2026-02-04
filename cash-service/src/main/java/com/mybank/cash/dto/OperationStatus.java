package com.mybank.cash.dto;

public enum OperationStatus {
    RESERVED,     // Создана, ожидает выполнения
    IN_PROGRESS, // Выполняется
    SUCCESS,     // Успешно завершена
    FAILED       // Ошибка
}
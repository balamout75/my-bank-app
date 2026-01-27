package com.mybank.template.util;

import com.mybank.template.exception.ValidationException;

import java.math.BigDecimal;

/**
 * Утилиты для валидации данных
 */
public class ValidationUtils {

    private ValidationUtils() {
        // Utility class
    }

    /**
     * Проверить что строка не пустая
     * 
     * @param value значение
     * @param fieldName имя поля (для сообщения об ошибке)
     * @throws ValidationException если значение пустое
     */
    public static void requireNonBlank(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new ValidationException(fieldName + " не может быть пустым");
        }
    }

    /**
     * Проверить что объект не null
     * 
     * @param value значение
     * @param fieldName имя поля (для сообщения об ошибке)
     * @throws ValidationException если значение null
     */
    public static void requireNonNull(Object value, String fieldName) {
        if (value == null) {
            throw new ValidationException(fieldName + " не может быть null");
        }
    }

    /**
     * Проверить что число положительное
     * 
     * @param value значение
     * @param fieldName имя поля (для сообщения об ошибке)
     * @throws ValidationException если значение <= 0
     */
    public static void requirePositive(BigDecimal value, String fieldName) {
        requireNonNull(value, fieldName);
        if (value.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException(fieldName + " должно быть положительным числом");
        }
    }

    /**
     * Проверить что число не отрицательное
     * 
     * @param value значение
     * @param fieldName имя поля (для сообщения об ошибке)
     * @throws ValidationException если значение < 0
     */
    public static void requireNonNegative(BigDecimal value, String fieldName) {
        requireNonNull(value, fieldName);
        if (value.compareTo(BigDecimal.ZERO) < 0) {
            throw new ValidationException(fieldName + " не может быть отрицательным");
        }
    }

    /**
     * Проверить что число больше или равно минимуму
     * 
     * @param value значение
     * @param min минимальное значение
     * @param fieldName имя поля (для сообщения об ошибке)
     * @throws ValidationException если value < min
     */
    public static void requireMinimum(BigDecimal value, BigDecimal min, String fieldName) {
        requireNonNull(value, fieldName);
        if (value.compareTo(min) < 0) {
            throw new ValidationException(
                fieldName + " должно быть не меньше " + min);
        }
    }

    /**
     * Проверить что число меньше или равно максимуму
     * 
     * @param value значение
     * @param max максимальное значение
     * @param fieldName имя поля (для сообщения об ошибке)
     * @throws ValidationException если value > max
     */
    public static void requireMaximum(BigDecimal value, BigDecimal max, String fieldName) {
        requireNonNull(value, fieldName);
        if (value.compareTo(max) > 0) {
            throw new ValidationException(
                fieldName + " должно быть не больше " + max);
        }
    }

    /**
     * Проверить что длина строки в заданных пределах
     * 
     * @param value значение
     * @param min минимальная длина
     * @param max максимальная длина
     * @param fieldName имя поля (для сообщения об ошибке)
     * @throws ValidationException если длина вне пределов
     */
    public static void requireLength(String value, int min, int max, String fieldName) {
        requireNonNull(value, fieldName);
        int length = value.length();
        if (length < min || length > max) {
            throw new ValidationException(
                String.format("%s должно быть длиной от %d до %d символов", 
                             fieldName, min, max));
        }
    }
}

package com.mybank.template.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.format.DateTimeFormatter;

/**
 * Утилиты для работы с датой и временем
 */
public class DateTimeUtils {

    private DateTimeUtils() {
        // Utility class
    }

    /**
     * Форматтер для дат в ISO формате (yyyy-MM-dd)
     */
    public static final DateTimeFormatter ISO_DATE_FORMATTER = 
            DateTimeFormatter.ISO_LOCAL_DATE;

    /**
     * Форматтер для даты и времени в ISO формате
     */
    public static final DateTimeFormatter ISO_DATETIME_FORMATTER = 
            DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    /**
     * Вычислить возраст по дате рождения
     * 
     * @param birthDate дата рождения
     * @return возраст в годах
     */
    public static int calculateAge(LocalDate birthDate) {
        if (birthDate == null) {
            throw new IllegalArgumentException("Дата рождения не может быть null");
        }
        
        LocalDate today = LocalDate.now();
        
        if (birthDate.isAfter(today)) {
            throw new IllegalArgumentException("Дата рождения не может быть в будущем");
        }
        
        return Period.between(birthDate, today).getYears();
    }

    /**
     * Проверить, является ли человек совершеннолетним (18+)
     * 
     * @param birthDate дата рождения
     * @return true если возраст >= 18
     */
    public static boolean isAdult(LocalDate birthDate) {
        return calculateAge(birthDate) >= 18;
    }

    /**
     * Форматировать дату в строку (yyyy-MM-dd)
     * 
     * @param date дата
     * @return отформатированная строка
     */
    public static String formatDate(LocalDate date) {
        return date != null ? date.format(ISO_DATE_FORMATTER) : null;
    }

    /**
     * Форматировать дату и время в строку
     * 
     * @param dateTime дата и время
     * @return отформатированная строка
     */
    public static String formatDateTime(LocalDateTime dateTime) {
        return dateTime != null ? dateTime.format(ISO_DATETIME_FORMATTER) : null;
    }

    /**
     * Получить текущую дату
     * 
     * @return LocalDate.now()
     */
    public static LocalDate today() {
        return LocalDate.now();
    }

    /**
     * Получить текущую дату и время
     * 
     * @return LocalDateTime.now()
     */
    public static LocalDateTime now() {
        return LocalDateTime.now();
    }
}

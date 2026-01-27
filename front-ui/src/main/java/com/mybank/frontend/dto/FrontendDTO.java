package com.mybank.frontend.dto;

import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * DTO для фронтенда
 */
public class FrontendDTO {

    /**
     * Информация об аккаунте пользователя
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AccountInfo {
        private Long id;
        private String username;
        
        @NotBlank(message = "Имя обязательно для заполнения")
        private String firstName;
        
        @NotBlank(message = "Фамилия обязательна для заполнения")
        private String lastName;
        
        @NotNull(message = "Дата рождения обязательна")
        @Past(message = "Дата рождения должна быть в прошлом")
        private LocalDate dateOfBirth;
        
        private BigDecimal balance;
        private Integer age;
    }

    /**
     * Форма обновления данных аккаунта
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AccountUpdateForm {
        @NotBlank(message = "Имя обязательно для заполнения")
        private String firstName;
        
        @NotBlank(message = "Фамилия обязательна для заполнения")
        private String lastName;
        
        @NotNull(message = "Дата рождения обязательна")
        @Past(message = "Дата рождения должна быть в прошлом")
        private LocalDate dateOfBirth;
    }

    /**
     * Краткая информация об аккаунте для списка переводов
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AccountSummary {
        private String username;
        private String fullName;
    }

    /**
     * Форма операции с деньгами (пополнение/снятие)
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CashOperationForm {
        @NotNull(message = "Сумма обязательна")
        @DecimalMin(value = "0.01", message = "Сумма должна быть больше 0")
        private BigDecimal amount;
    }

    /**
     * Форма перевода денег
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TransferForm {
        @NotBlank(message = "Выберите получателя")
        private String toUsername;
        
        @NotNull(message = "Сумма обязательна")
        @DecimalMin(value = "0.01", message = "Сумма должна быть больше 0")
        private BigDecimal amount;
    }

    /**
     * Модель для главной страницы
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MainPageModel {
        private AccountInfo account;
        private List<AccountSummary> availableAccounts;
        private String errorMessage;
        private String successMessage;
    }
}

package com.mybank.accounts.service;

import com.mybank.accounts.dto.BalanceUpdateRequest;
import com.mybank.accounts.dto.CashOperationType;
import com.mybank.accounts.exception.AccountNotFoundException;
import com.mybank.accounts.exception.InsufficientFundsException;
import com.mybank.accounts.model.UserAccount;
import com.mybank.accounts.repository.AccountRepository;
import com.mybank.accounts.repository.ServiceOperationsRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CashServiceTest {

    @Mock AccountRepository accountRepository;
    @Mock ServiceOperationsRepository appliedRepo;

    @InjectMocks CashService cashService;

    private UserAccount alice(BigDecimal balance) {
        return UserAccount.builder()
                .id(1L).userName("alice").firstName("Alice").lastName("Smith")
                .email("alice@mybank.com").dateOfBirth(java.time.LocalDate.of(1990, 1, 1))
                .balance(balance).build();
    }

    // ======================== DEPOSIT ========================

    @Test
    @DisplayName("DEPOSIT → баланс увеличивается на сумму операции")
    void deposit_shouldIncreaseBalance() {
        when(appliedRepo.insertIfAbsent(1L, "alice", "cash-service")).thenReturn(true);
        UserAccount alice = alice(new BigDecimal("1000.00"));
        when(accountRepository.findByUserName("alice")).thenReturn(Optional.of(alice));

        var req = new BalanceUpdateRequest("alice", new BigDecimal("250.00"), CashOperationType.DEPOSIT, 1L);
        cashService.applyBalance(req, "cash-service");

        assertThat(alice.getBalance()).isEqualByComparingTo("1250.00");
        verify(accountRepository).save(alice);
    }

    // ======================== WITHDRAW ========================

    @Test
    @DisplayName("WITHDRAW → баланс уменьшается на сумму операции")
    void withdraw_shouldDecreaseBalance() {
        when(appliedRepo.insertIfAbsent(2L, "alice", "cash-service")).thenReturn(true);
        UserAccount alice = alice(new BigDecimal("1000.00"));
        when(accountRepository.findByUserName("alice")).thenReturn(Optional.of(alice));

        var req = new BalanceUpdateRequest("alice", new BigDecimal("300.00"), CashOperationType.WITHDRAW, 2L);
        cashService.applyBalance(req, "cash-service");

        assertThat(alice.getBalance()).isEqualByComparingTo("700.00");
        verify(accountRepository).save(alice);
    }

    // ======================== INSUFFICIENT FUNDS ========================

    @Test
    @DisplayName("WITHDRAW при недостатке средств → InsufficientFundsException")
    void withdraw_insufficientFunds_shouldThrow() {
        when(appliedRepo.insertIfAbsent(3L, "alice", "cash-service")).thenReturn(true);
        UserAccount alice = alice(new BigDecimal("50.00"));
        when(accountRepository.findByUserName("alice")).thenReturn(Optional.of(alice));

        var req = new BalanceUpdateRequest("alice", new BigDecimal("100.00"), CashOperationType.WITHDRAW, 3L);

        assertThatThrownBy(() -> cashService.applyBalance(req, "cash-service"))
                .isInstanceOf(InsufficientFundsException.class);
        verify(accountRepository, never()).save(any());
    }

    // ======================== IDEMPOTENCY ========================

    @Test
    @DisplayName("Повторный вызов с тем же operationId → операция пропускается, баланс не меняется")
    void duplicateOperation_shouldBeSkipped() {
        when(appliedRepo.insertIfAbsent(4L, "alice", "cash-service")).thenReturn(false);

        var req = new BalanceUpdateRequest("alice", new BigDecimal("500.00"), CashOperationType.DEPOSIT, 4L);
        cashService.applyBalance(req, "cash-service");

        verify(accountRepository, never()).findByUserName(any());
        verify(accountRepository, never()).save(any());
    }

    // ======================== ACCOUNT NOT FOUND ========================

    @Test
    @DisplayName("Пользователь не найден → AccountNotFoundException")
    void userNotFound_shouldThrow() {
        when(appliedRepo.insertIfAbsent(5L, "unknown", "cash-service")).thenReturn(true);
        when(accountRepository.findByUserName("unknown")).thenReturn(Optional.empty());

        var req = new BalanceUpdateRequest("unknown", new BigDecimal("100.00"), CashOperationType.DEPOSIT, 5L);

        assertThatThrownBy(() -> cashService.applyBalance(req, "cash-service"))
                .isInstanceOf(AccountNotFoundException.class);
    }
}

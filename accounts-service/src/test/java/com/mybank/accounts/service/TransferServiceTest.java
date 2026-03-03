package com.mybank.accounts.service;

import com.mybank.accounts.dto.TransferConsumeRequest;
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
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransferServiceTest {

    @Mock AccountRepository accountRepository;
    @Mock ServiceOperationsRepository appliedRepo;

    @InjectMocks TransferService transferService;

    private UserAccount user(String name, BigDecimal balance) {
        return UserAccount.builder()
                .userName(name).firstName(name).lastName("Test")
                .email(name + "@mybank.com").dateOfBirth(LocalDate.of(1990, 1, 1))
                .balance(balance).build();
    }

    // ======================== SUCCESSFUL TRANSFER ========================

    @Test
    @DisplayName("Перевод → у отправителя списывается, у получателя зачисляется")
    void transfer_shouldMoveMoneyBetweenAccounts() {
        when(appliedRepo.insertIfAbsent(1L, "alice", "transfer-service")).thenReturn(true);
        UserAccount alice = user("alice", new BigDecimal("1000.00"));
        UserAccount bob = user("bob", new BigDecimal("500.00"));
        when(accountRepository.findByUserName("alice")).thenReturn(Optional.of(alice));
        when(accountRepository.findByUserName("bob")).thenReturn(Optional.of(bob));

        var req = new TransferConsumeRequest(1L, "alice", "bob", new BigDecimal("200.00"));
        transferService.transfer(req, "transfer-service");

        assertThat(alice.getBalance()).isEqualByComparingTo("800.00");
        assertThat(bob.getBalance()).isEqualByComparingTo("700.00");
        verify(accountRepository).save(alice);
        verify(accountRepository).save(bob);
    }

    // ======================== IDEMPOTENCY ========================

    @Test
    @DisplayName("Повторный перевод с тем же operationId → пропускается, балансы не меняются")
    void duplicateTransfer_shouldBeSkipped() {
        when(appliedRepo.insertIfAbsent(2L, "alice", "transfer-service")).thenReturn(false);

        var req = new TransferConsumeRequest(2L, "alice", "bob", new BigDecimal("100.00"));
        transferService.transfer(req, "transfer-service");

        verify(accountRepository, never()).findByUserName(any());
        verify(accountRepository, never()).save(any());
    }

    // ======================== SELF-TRANSFER ========================

    @Test
    @DisplayName("Перевод самому себе → IllegalArgumentException")
    void selfTransfer_shouldThrow() {
        when(appliedRepo.insertIfAbsent(3L, "alice", "transfer-service")).thenReturn(true);

        var req = new TransferConsumeRequest(3L, "alice", "alice", new BigDecimal("100.00"));

        assertThatThrownBy(() -> transferService.transfer(req, "transfer-service"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("самому себе");
    }

    // ======================== INSUFFICIENT FUNDS ========================

    @Test
    @DisplayName("Недостаточно средств у отправителя → InsufficientFundsException")
    void insufficientFunds_shouldThrow() {
        when(appliedRepo.insertIfAbsent(4L, "alice", "transfer-service")).thenReturn(true);
        UserAccount alice = user("alice", new BigDecimal("50.00"));
        UserAccount bob = user("bob", new BigDecimal("500.00"));
        when(accountRepository.findByUserName("alice")).thenReturn(Optional.of(alice));
        when(accountRepository.findByUserName("bob")).thenReturn(Optional.of(bob));

        var req = new TransferConsumeRequest(4L, "alice", "bob", new BigDecimal("100.00"));

        assertThatThrownBy(() -> transferService.transfer(req, "transfer-service"))
                .isInstanceOf(InsufficientFundsException.class);
        verify(accountRepository, never()).save(any());
    }

    // ======================== ACCOUNT NOT FOUND ========================

    @Test
    @DisplayName("Отправитель не найден → AccountNotFoundException")
    void senderNotFound_shouldThrow() {
        when(appliedRepo.insertIfAbsent(5L, "unknown", "transfer-service")).thenReturn(true);
        when(accountRepository.findByUserName("unknown")).thenReturn(Optional.empty());

        var req = new TransferConsumeRequest(5L, "unknown", "bob", new BigDecimal("100.00"));

        assertThatThrownBy(() -> transferService.transfer(req, "transfer-service"))
                .isInstanceOf(AccountNotFoundException.class);
    }

    @Test
    @DisplayName("Получатель не найден → AccountNotFoundException")
    void recipientNotFound_shouldThrow() {
        when(appliedRepo.insertIfAbsent(6L, "alice", "transfer-service")).thenReturn(true);
        UserAccount alice = user("alice", new BigDecimal("1000.00"));
        when(accountRepository.findByUserName("alice")).thenReturn(Optional.of(alice));
        when(accountRepository.findByUserName("ghost")).thenReturn(Optional.empty());

        var req = new TransferConsumeRequest(6L, "alice", "ghost", new BigDecimal("100.00"));

        assertThatThrownBy(() -> transferService.transfer(req, "transfer-service"))
                .isInstanceOf(AccountNotFoundException.class);
    }
}

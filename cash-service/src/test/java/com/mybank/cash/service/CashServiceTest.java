package com.mybank.cash.service;

import com.mybank.cash.client.AccountsClient;
import com.mybank.cash.client.NotificationsClient;
import com.mybank.cash.dto.CashOperationRequest;
import com.mybank.cash.dto.CashOperationType;
import com.mybank.cash.dto.OperationStatus;
import com.mybank.cash.model.CashOperation;
import com.mybank.cash.repository.CashOperationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.mockito.Mockito.*;

@ExtendWith(org.mockito.junit.jupiter.MockitoExtension.class)
class CashServiceTest {

    @Mock CashOperationRepository repository;
    @Mock AccountsClient accountsClient;
    @Mock NotificationsClient notificationsClient;
    @InjectMocks CashService service;

    @Test


    /*
    * Executes the business workflow of a cash operation:
    * updates state, calls external services, and persists all lifecycle changes.
    */
    void operate() {
        var request = new CashOperationRequest(1L, CashOperationType.DEPOSIT, new BigDecimal(100));

        var existing = CashOperation.builder()
                .operationId(1L)
                .username("alice")
                .amount(new BigDecimal("100.00"))
                .type(CashOperationType.DEPOSIT)
                .status(OperationStatus.RESERVED)
                .createdAt(LocalDateTime.now())
                .notificationAttemptsAt(LocalDateTime.now())
                .notificationAttempts(0)
                .build();

        when(repository.findById(1L)).thenReturn(Optional.of(existing));

        service.operate("alice",request);
        verify(repository, times(2)).save(any(CashOperation.class));
        verify(accountsClient).updateBalance(any());
    }
}

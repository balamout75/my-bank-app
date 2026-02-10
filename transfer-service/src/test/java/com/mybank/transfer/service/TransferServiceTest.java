package com.mybank.transfer.service;

import com.mybank.transfer.client.AccountsClient;
import com.mybank.transfer.client.NotificationsClient;
import com.mybank.transfer.dto.TransferConsumeRequest;
import com.mybank.transfer.dto.TransferOperationRequest;
import com.mybank.transfer.dto.OperationStatus;
import com.mybank.transfer.model.TransferOperation;
import com.mybank.transfer.repository.TransferOperationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.mockito.Mockito.*;

@ExtendWith(org.mockito.junit.jupiter.MockitoExtension.class)
class TransferServiceTest {

    @Mock TransferOperationRepository repository;
    @Mock AccountsClient accountsClient;
    @Mock NotificationsClient notificationsClient;
    @InjectMocks TransferService service;

    @Test


    /*
    * Executes the business workflow of a cash operation:
    * updates state, calls external services, and persists all lifecycle changes.
    */
    void operate() {
        var request = new TransferOperationRequest(1L, "bob", new BigDecimal(100));

        var existing = TransferOperation.builder()
                .operationId(1L)
                .username("alice")
                .recipient("bob")
                .amount(new BigDecimal("100.00"))
                .status(OperationStatus.RESERVED)
                .createdAt(LocalDateTime.now())
                .notificationAttemptsAt(LocalDateTime.now())
                .notificationAttempts(0)
                .build();

        when(repository.findById(1L)).thenReturn(Optional.of(existing));

        service.transfer("alice",request);
        verify(repository, times(2)).save(any(TransferOperation.class));
        verify(accountsClient).transfer(any(TransferConsumeRequest.class));
    }
}

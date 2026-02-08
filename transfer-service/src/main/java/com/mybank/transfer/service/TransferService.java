package com.mybank.transfer.service;

import com.mybank.transfer.client.AccountsClient;
import com.mybank.transfer.client.NotificationsClient;
import com.mybank.transfer.dto.*;
import com.mybank.transfer.exception.InvalidOperationKeyException;
import com.mybank.transfer.model.TransferOperation;
import com.mybank.transfer.repository.TransferOperationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransferService {

    private final TransferOperationRepository operationRepository;
    private final AccountsClient accountsClient;
    private final NotificationsClient notificationsClient;


    /**
     * Генерирует новый ключ операции
     */
    public OperationKeyResponse generateOperationKey(String username) {
        Long operationId = operationRepository.getNextOperationId();
        TransferOperation op = TransferOperation.builder()
                .operationId(operationId)
                .username(username)
                .recipient(username)
                .status(OperationStatus.RESERVED)
                .createdAt(LocalDateTime.now())
                .notificationAttempts(0)
                .notificationAttemptsAt(LocalDateTime.now())
                .build();
        operationRepository.save(op);
        return new OperationKeyResponse(operationId);
    }

    @Transactional
    public void transfer(String username, TransferOperationRequest request) {
        executeOperation(username, request);
    }

    @Transactional(readOnly = true)
    public TransferOperation getOperation(Long operationId) {
        return operationRepository.findById(operationId)
                .orElseThrow(() -> new InvalidOperationKeyException(
                        "Операция не найдена: " + operationId));
    }

    // ==================== PRIVATE ====================

    private void executeOperation(String username, TransferOperationRequest request) {
        Long operationId = request.operationId();

        var existingOpt = operationRepository.findById(operationId);
        if (existingOpt.isPresent()) {
            TransferOperation op = existingOpt.get();
            // username всегда должен совпадать
            if (!op.getUsername().equals(username)) {
                throw new InvalidOperationKeyException("OperationId принадлежит другому пользователю: " + operationId);
            }
            if (op.getStatus() == OperationStatus.RESERVED) {
                op.setRecipient(request.recipient());
                op.setAmount(request.amount());
                processOperation(op);
                return;
            }
        }
        throw new InvalidOperationKeyException("Operation key не зарезервирован: " + operationId);
    }

    private void processOperation(TransferOperation operation) {
        operation.setStatus(OperationStatus.IN_PROGRESS);
        operationRepository.save(operation);

        log.info("🚀 Executing transfer: user={}, recipient={}, amount={}, operationId={}",
                operation.getUsername(), operation.getRecipient(),
                operation.getAmount(), operation.getOperationId());

        try {
            accountsClient.transfer(new TransferConsumeRequest(
                    operation.getOperationId(),
                    operation.getUsername(),
                    operation.getRecipient(),
                    operation.getAmount()

            ));
            operation.setStatus(OperationStatus.UPDATED);
            operation.setCompletedAt(LocalDateTime.now());
            operationRepository.save(operation);
            log.info("✅ Operation SUCCESS: {}", operation.getOperationId());
        } catch (Exception e) {
            operation.setStatus(OperationStatus.FAILED);
            operation.setCompletedAt(LocalDateTime.now());
            operation.setErrorMessage(e.getMessage());
            operationRepository.save(operation);
            log.error("❌ Operation FAILED: id={}, error={}", operation.getOperationId(), e.getMessage(), e);
            throw e;
        }
    }
}
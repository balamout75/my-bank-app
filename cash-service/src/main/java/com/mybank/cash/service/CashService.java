package com.mybank.cash.service;

import com.mybank.cash.client.AccountsClient;
import com.mybank.cash.dto.BalanceUpdateRequest;
import com.mybank.cash.dto.CashOperationRequest;
import com.mybank.cash.dto.CashOperationType;
import com.mybank.cash.dto.OperationKeyResponse;
import com.mybank.cash.dto.OperationStatus;
import com.mybank.cash.dto.*;
import com.mybank.cash.exception.InvalidOperationKeyException;
import com.mybank.cash.model.CashOperation;
import com.mybank.cash.repository.CashOperationRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@Slf4j
public class CashService {

    private final CashOperationRepository operationRepository;
    private final AccountsClient accountsClient;
    private final MeterRegistry meterRegistry;

    public CashService(CashOperationRepository operationRepository,
                       AccountsClient accountsClient,
                       MeterRegistry meterRegistry) {
        this.operationRepository = operationRepository;
        this.accountsClient = accountsClient;
        this.meterRegistry = meterRegistry;
    }


    /**
     * Генерирует новый ключ операции
     */
    public OperationKeyResponse generateOperationKey(String username) {
        Long operationId = operationRepository.getNextOperationId();
        CashOperation op = CashOperation.builder()
                .username(username)
                .operationId(operationId)
                .status(OperationStatus.RESERVED)
                .createdAt(LocalDateTime.now())
                .notificationAttempts(0)
                .notificationAttemptsAt(LocalDateTime.now())
                .build();
        operationRepository.save(op);
        return new OperationKeyResponse(operationId);
    }

    @Transactional
    public void operate(String username, CashOperationRequest request) {
        executeOperation(username, request);
    }

    @Transactional(readOnly = true)
    public CashOperation getOperation(Long operationId) {
        return operationRepository.findById(operationId)
                .orElseThrow(() -> new InvalidOperationKeyException(
                        "Операция не найдена: " + operationId));
    }

    // ==================== PRIVATE ====================

    private void executeOperation(String username, CashOperationRequest request) {

        Long operationId = request.operationId();
        var existingOpt = operationRepository.findById(operationId);
        if (existingOpt.isPresent()) {
            CashOperation op = existingOpt.get();
            // username всегда должен совпадать
            if (!op.getUsername().equals(username)) {
                throw new InvalidOperationKeyException("OperationId принадлежит другому пользователю: " + operationId);
            }
            if (op.getStatus() == OperationStatus.RESERVED) {
                op.setType(request.cashOperationType());
                op.setAmount(request.amount());
                processOperation(op);
                return;
            }
        }
        throw new InvalidOperationKeyException("Operation key не зарезервирован: " + operationId);
    }

    private void processOperation(CashOperation operation) {
        operation.setStatus(OperationStatus.IN_PROGRESS);
        operationRepository.save(operation);

        log.info("🚀 Executing {}: user={}, amount={}, operationId={}",
                operation.getType(), operation.getUsername(),
                operation.getAmount(), operation.getOperationId());

        try {
            accountsClient.updateBalance(new BalanceUpdateRequest(
                    operation.getUsername(),
                    operation.getAmount(),
                    operation.getType(),
                    operation.getOperationId()
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

            if (operation.getType() == CashOperationType.WITHDRAW) {
                Counter.builder("cash_withdraw_failed_total")
                        .description("Number of failed withdrawal attempts")
                        .tag("username", operation.getUsername())
                        .register(meterRegistry)
                        .increment();
            }

            log.error("❌ Operation FAILED: id={}, error={}", operation.getOperationId(), e.getMessage(), e);
            throw e;
        }
    }
}
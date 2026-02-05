package com.mybank.cash.service;

import com.mybank.cash.client.AccountsClient;
import com.mybank.cash.client.NotificationsClient;
import com.mybank.cash.dto.*;
import com.mybank.cash.exception.InvalidOperationKeyException;
import com.mybank.cash.model.CashOperation;
import com.mybank.cash.repository.CashOperationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class CashService {

    private final CashOperationRepository operationRepository;
    private final AccountsClient accountsClient;
    private final NotificationsClient notificationsClient;


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

            // если RESERVED — разрешаем "дозаполнить"
            if (op.getStatus() == OperationStatus.RESERVED) {

                if (op.getType() == null) {
                    op.setType(request.operationType());
                } else if (op.getType() != request.operationType()) {
                    throw new InvalidOperationKeyException("OperationId уже использован с другим type: " + operationId);
                }

                if (op.getAmount() == null) {
                    op.setAmount(request.amount());
                } else if (op.getAmount().compareTo(request.amount()) != 0) {
                    throw new InvalidOperationKeyException("OperationId уже использован с другой суммой: " + operationId);
                }

                // теперь можно продолжать выполнение
                processOperation(op);
                return;
            }

            // не RESERVED — параметры обязаны совпасть
            if (op.getType() != request.operationType()
                    || op.getAmount().compareTo(request.amount()) != 0) {
                throw new InvalidOperationKeyException("OperationId уже использован с другими параметрами: " + operationId);
            }

            if (op.getStatus() == OperationStatus.SUCCESS) return;
            if (op.getStatus() == OperationStatus.IN_PROGRESS) {
                throw new InvalidOperationKeyException("Операция уже выполняется: " + operationId);
            }

            // FAILED -> разрешаем повтор
            processOperation(op);
            return;
        }


        // ⚠ Если записи нет — значит ключ не резервировался
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

            operation.setStatus(OperationStatus.SUCCESS);
            operation.setCompletedAt(LocalDateTime.now());
            operationRepository.save(operation);

            sendNotification(operation.getOperationId(), operation.getUsername(), operation.getAmount(), operation.getType());

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


    private void sendNotification(Long operationId, String username, BigDecimal amount, OperationType type) {
        try {
            notificationsClient.send(new NotificationRequest(
                    operationId,
                    "CASH_" + type.name(),
                    username,
                    type.name().toLowerCase() + " completed",
                    Map.of("amount", amount)
            ));
        } catch (Exception e) {
            log.warn("⚠️ Failed to send notification: {}", e.getMessage());
        }
    }
}
package com.mybank.accounts.service;

import com.mybank.accounts.dto.BalanceUpdateRequest;
import com.mybank.accounts.exception.AccountNotFoundException;
import com.mybank.accounts.exception.InsufficientFundsException;
import com.mybank.accounts.model.UserAccount;
import com.mybank.accounts.repository.AccountRepository;
import com.mybank.accounts.repository.ServiceOperationsRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class CashService {

    private static final Logger log = LoggerFactory.getLogger(CashService.class);
    private final ServiceOperationsRepository appliedRepo;
    private final AccountRepository accountRepository;

    /**
     * Применяет операцию изменения баланса.
     *
     * @param req      запрос на изменение баланса
     * @param clientId ID клиента (сервиса), от которого пришёл запрос (для аудита)
     */
    @Transactional
    public void applyBalance(BalanceUpdateRequest req, String clientId) {
        // Идемпотентность: проверяем, не применяли ли уже эту операцию
        boolean firstTime = appliedRepo.insertIfAbsent(req.operationId(), req.username(), clientId);
        if (!firstTime) {
            log.info("⏭️ Операция {} уже применена (client={}), пропускаем",
                    req.operationId(), clientId);
            return;
        }

        // Находим аккаунт
        UserAccount account = accountRepository.findByUserName(req.username())
                .orElseThrow(() -> new AccountNotFoundException(req.username()));

        BigDecimal currentBalance = account.getBalance();
        BigDecimal newBalance;

        switch (req.cashOperationType()) {
            case DEPOSIT -> {
                newBalance = currentBalance.add(req.amount());
                log.info("💰 DEPOSIT: user={}, amount={}, balance: {} -> {}, client={}",
                        req.username(), req.amount(), currentBalance, newBalance, clientId);
            }
            case WITHDRAW -> {
                // Проверка на достаточность средств
                if (currentBalance.compareTo(req.amount()) < 0) {
                    throw new InsufficientFundsException(currentBalance, req.amount());
                }
                newBalance = currentBalance.subtract(req.amount());
                log.info("💸 WITHDRAW: user={}, amount={}, balance: {} -> {}, client={}",
                        req.username(), req.amount(), currentBalance, newBalance, clientId);
            }
            default -> throw new IllegalArgumentException("Unknown operation type: " + req.cashOperationType());
        }

        account.setBalance(newBalance);
        accountRepository.save(account);

        log.info("✅ Баланс обновлён: user={}, operationId={}, newBalance={}, client={}",
                req.username(), req.operationId(), newBalance, clientId);
    }
}
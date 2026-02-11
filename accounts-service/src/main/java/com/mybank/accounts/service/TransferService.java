package com.mybank.accounts.service;

import com.mybank.accounts.dto.TransferConsumeRequest;
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
public class TransferService {

    private static final Logger log = LoggerFactory.getLogger(TransferService.class);
    private final ServiceOperationsRepository appliedRepo;
    private final AccountRepository accountRepository;

    /**
     * Выполняет перевод денег между пользователями.
     *
     * @param req      запрос на перевод
     * @param clientId ID клиента (сервиса), от которого пришёл запрос (для аудита)
     */
    @Transactional
    public void transfer(TransferConsumeRequest req, String clientId) {
        // Идемпотентность: проверяем, не применяли ли уже эту операцию
        boolean firstTime = appliedRepo.insertIfAbsent(req.operationId(), req.username(), clientId);
        if (!firstTime) {
            log.info("⏭️ Перевод {} уже выполнен (client={}), пропускаем",
                    req.operationId(), clientId);
            return;
        }

        // Нельзя переводить самому себе
        if (req.username().equals(req.recipient())) {
            throw new IllegalArgumentException("Нельзя переводить самому себе");
        }

        // Находим отправителя
        UserAccount sender = accountRepository.findByUserName(req.username())
                .orElseThrow(() -> new AccountNotFoundException(req.username()));

        // Находим получателя
        UserAccount recipient = accountRepository.findByUserName(req.recipient())
                .orElseThrow(() -> new AccountNotFoundException(req.recipient()));

        BigDecimal amount = req.amount();
        BigDecimal senderBalance = sender.getBalance();

        // Проверка на достаточность средств
        if (senderBalance.compareTo(amount) < 0) {
            throw new InsufficientFundsException(senderBalance, amount);
        }

        // Списание со счёта отправителя
        BigDecimal newSenderBalance = senderBalance.subtract(amount);
        sender.setBalance(newSenderBalance);

        // Зачисление на счёт получателя
        BigDecimal newRecipientBalance = recipient.getBalance().add(amount);
        recipient.setBalance(newRecipientBalance);

        // Сохраняем оба аккаунта
        accountRepository.save(sender);
        accountRepository.save(recipient);

        log.info("💸 TRANSFER: from={} to={}, amount={}, senderBalance: {} -> {}, recipientBalance: {} -> {}, client={}",
                req.username(), req.recipient(), amount,
                senderBalance, newSenderBalance,
                recipient.getBalance().subtract(amount), newRecipientBalance,
                clientId);

        log.info("✅ Перевод выполнен: operationId={}, from={}, to={}, amount={}, client={}",
                req.operationId(), req.username(), req.recipient(), amount, clientId);
    }
}
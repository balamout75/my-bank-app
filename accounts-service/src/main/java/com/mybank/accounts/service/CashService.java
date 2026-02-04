package com.mybank.accounts.service;

import com.mybank.accounts.dto.BalanceUpdateRequest;
import com.mybank.accounts.repository.AccountRepository;
import com.mybank.accounts.repository.AppliedOperationsRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CashService {

    private static final Logger log = LoggerFactory.getLogger(CashService.class);
    private final AppliedOperationsRepository appliedRepo;
    private final AccountRepository accountRepository; // твой репо для аккаунта

    @Transactional
    public void applyBalance(BalanceUpdateRequest req) {
        boolean firstTime = appliedRepo.insertIfAbsent(req.operationId(), req.username());
        if (!firstTime) {
            // уже применяли — идемпотентный replay
            return;
        }

        switch (req.operationType()) {
            case DEPOSIT -> log.info("добавили");
            case WITHDRAW -> log.info("убавили");
        }
    }
}

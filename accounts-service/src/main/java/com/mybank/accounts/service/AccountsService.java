package com.mybank.accounts.service;

import com.mybank.accounts.client.notifications.NotificationsClient;
import com.mybank.accounts.dto.AccountMeResponse;
import com.mybank.accounts.dto.AccountSummaryResponse;
import com.mybank.accounts.dto.AccountUpdateRequest;
import com.mybank.accounts.model.AccountOperation;
import com.mybank.accounts.model.OperationStatus;
import com.mybank.accounts.model.UserAccount;
import com.mybank.accounts.repository.AccountRepository;
import com.mybank.accounts.repository.AccountOperationRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.Period;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AccountsService {

    private static final Logger log = LoggerFactory.getLogger(AccountsService.class);
    private final AccountRepository repo;
    private final NotificationsClient notificationsClient;
    private final AccountOperationRepository accountOperationRepository;

    private static AccountMeResponse toMeResponse(UserAccount u) {
        return AccountMeResponse.builder()
                .username(u.getUserName())
                .firstName(u.getFirstName())
                .lastName(u.getLastName())
                .dateOfBirth(u.getDateOfBirth())
                .balance(u.getBalance())
                .build();
    }

    @Transactional(readOnly = true)
    public AccountMeResponse getMe(String username) {
        UserAccount u = repo.findByUserName(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));

        return toMeResponse(u);
    }

    @Transactional
    public void updateMe(String username, AccountUpdateRequest req) {
        validateAdult(req.dateOfBirth());

        long opId = (req.operationId() != null) ? req.operationId() : accountOperationRepository.nextOperationId();
        AccountOperation op = accountOperationRepository.findById(opId).orElseGet(() -> {
            var x = new AccountOperation();
            x.setOperationId(opId);
            x.setUsername(username);
            x.setPayload(buildPayload(req));
            x.setAttempts(0);
            x.setError(null);
            x.setStatus(OperationStatus.RECEIVED);
            x.touch();
            return accountOperationRepository.save(x);
        });

        UserAccount u = repo.findByUserName(username).orElseThrow(() -> new IllegalArgumentException("User not found: " + username));
        u.setFirstName(req.firstName());
        u.setLastName(req.lastName());
        u.setDateOfBirth(req.dateOfBirth());
        repo.save(u);

        op.setStatus(OperationStatus.UPDATED);
        op.setError(null);
        op.touch();
        accountOperationRepository.save(op);

    }

    private Map<String, Object> buildPayload(AccountUpdateRequest req) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("firstName", req.firstName());
        payload.put("lastName", req.lastName());
        payload.put("dateOfBirth", req.dateOfBirth());
        return payload;
    }

    @Transactional(readOnly = true)
    public List<AccountSummaryResponse> getAllOthers(String username) {
        return repo.findAllByUserNameNot(username).stream()
                .map(u -> new AccountSummaryResponse(u.getUserName(), u.getFirstName() + " " + u.getLastName()))
                .toList();
    }

    private void validateAdult(LocalDate dob) {
        int years = Period.between(dob, LocalDate.now()).getYears();
        if (years < 18) {
            throw new IllegalArgumentException("Возраст должен быть 18+");
        }
    }
}

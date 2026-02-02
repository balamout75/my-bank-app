package com.mybank.accounts.service;

import com.mybank.accounts.dto.*;
import com.mybank.accounts.client.notifications.NotificationsClient;
import com.mybank.accounts.client.notifications.dto.NotificationRequest;
import com.mybank.accounts.dto.AccountMeResponse;
import com.mybank.accounts.dto.AccountSummaryResponse;
import com.mybank.accounts.dto.AccountUpdateRequest;
import com.mybank.accounts.model.UserAccount;
import com.mybank.accounts.repository.UserAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AccountsService {

    private final UserAccountRepository repo;
    private final NotificationsClient notificationsClient;

    @Transactional(readOnly = true)
    public AccountMeResponse getMe(String username) {
        UserAccount u = repo.findByUserName(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));

        return toMeResponse(u);
    }

    @Transactional
    public void updateMe(String username, AccountUpdateRequest req) {
        validateAdult(req.dateOfBirth());

        UserAccount u = repo.findByUserName(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));

        u.setFirstName(req.firstName());
        u.setLastName(req.lastName());
        u.setDateOfBirth(req.dateOfBirth());
        repo.save(u);

        // MVP: синхронно пишем уведомление (позже заменим на outbox/очередь)
        //notificationsClient.send(new NotificationRequest(
        //        "ACCOUNT_UPDATED",
        //        username,
        //        "Account profile updated",
        //        Map.of("firstName", req.firstName(), "lastName", req.lastName())
        //));
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

    private static AccountMeResponse toMeResponse(UserAccount u) {
        return AccountMeResponse.builder()
                .id(u.getId())
                .username(u.getUserName())
                .firstName(u.getFirstName())
                .lastName(u.getLastName())
                .email(u.getEmail())
                .dateOfBirth(u.getDateOfBirth())
                .balance(u.getBalance())
                .build();
    }
}

package com.mybank.accounts.controller;

import com.mybank.accounts.dto.*;
import com.mybank.accounts.service.AccountsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/accounts")
@RequiredArgsConstructor
public class AccountsController {

    private final AccountsService accountsService;

    // MVP: берём username из Authentication (в будущем JWT даст его же)
    private String username(Authentication auth) {
        return (auth != null) ? auth.getName() : "alice";
    }

    @GetMapping("/me")
    public AccountMeResponse me(Authentication auth) {
        return accountsService.getMe(username(auth));
    }

    @PutMapping("/me")
    public ResponseEntity<Void> updateMe(@Valid @RequestBody AccountUpdateRequest req, Authentication auth) {
        accountsService.updateMe(username(auth), req);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/all")
    public List<AccountSummaryResponse> all(Authentication auth) {
        return accountsService.getAllOthers(username(auth));
    }
}

package com.mybank.accounts.controller;

import com.mybank.accounts.dto.*;
import com.mybank.accounts.service.AccountsService;
import com.mybank.accounts.service.CashService;
import com.mybank.accounts.service.TransferService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/accounts")  // ← Убрали /api
@RequiredArgsConstructor
public class AccountsController {

    private final AccountsService accountsService;
    private final CashService cashService;
    private final TransferService transferService;

    @GetMapping("/me")
    public AccountMeResponse me(@AuthenticationPrincipal Jwt jwt) {
        String username = extractUsername(jwt);
        return accountsService.getMe(username);
    }

    @PutMapping("/me")
    public ResponseEntity<Void> updateMe(@AuthenticationPrincipal Jwt jwt,
                                      @Valid @RequestBody AccountUpdateRequest req) {
        String username = extractUsername(jwt);
        accountsService.updateMe(username, req);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/all")
    public List<AccountSummaryResponse> others(@AuthenticationPrincipal Jwt jwt) {
        String username = extractUsername(jwt);
        return accountsService.getAllOthers(username);
    }

    @PostMapping("/balance")
    public ResponseEntity<Void> balance(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody BalanceUpdateRequest req
    ) {
        String clientId = extractClientId(jwt);
        cashService.applyBalance(req, clientId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/transfer")
    public ResponseEntity<Void> transfer(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody TransferConsumeRequest req
    ) {
        String clientId = extractClientId(jwt);
        transferService.transfer(req, clientId);
        return ResponseEntity.noContent().build();
    }


    private String extractUsername(Jwt jwt) {
        // 1. preferred_username (стандартный claim Keycloak)
        String username = jwt.getClaimAsString("preferred_username");
        if (username != null && !username.isEmpty()) {
            return username;
        }
        username = jwt.getClaimAsString("name");
        if (username != null && !username.isEmpty()) {
            return username;
        }
        username = jwt.getClaimAsString("email");
        if (username != null && !username.isEmpty()) {
            return username;
        }
        return jwt.getSubject();
    }

    private String extractClientId(Jwt jwt) {
        String clientId = jwt.getClaimAsString("azp");
        if (clientId != null && !clientId.isEmpty()) {
            return clientId;
        }
        clientId = jwt.getClaimAsString("client_id");
        if (clientId != null && !clientId.isEmpty()) {
            return clientId;
        }
        return jwt.getSubject();
    }
}
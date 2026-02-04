package com.mybank.accounts.controller;

import com.mybank.accounts.dto.AccountMeResponse;
import com.mybank.accounts.dto.AccountSummaryResponse;
import com.mybank.accounts.dto.AccountUpdateRequest;
import com.mybank.accounts.dto.BalanceUpdateRequest;
import com.mybank.accounts.service.AccountsService;
import com.mybank.accounts.service.CashService;
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

    private final CashService cashService;
    private final AccountsService accountsService;

    @GetMapping("/me")
    public AccountMeResponse me(@AuthenticationPrincipal Jwt jwt) {
        String username = extractUsername(jwt);
        return accountsService.getMe(username);
    }

    @PutMapping("/me")
    public ResponseEntity<Void> updateMe(
            @AuthenticationPrincipal Jwt jwt,
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
            @Valid @RequestBody BalanceUpdateRequest req
            // можно также принять Jwt jwt и проверять роли: ROLE_cash-service или scope
    ) {
        cashService.applyBalance(req);
        return ResponseEntity.noContent().build();
    }


    /**
     * Извлекает username из JWT токена Keycloak
     */
    private String extractUsername(Jwt jwt) {
        // 1. preferred_username (стандартный claim Keycloak)
        String username = jwt.getClaimAsString("preferred_username");
        if (username != null && !username.isEmpty()) {
            return username;
        }

        // 2. name
        username = jwt.getClaimAsString("name");
        if (username != null && !username.isEmpty()) {
            return username;
        }

        // 3. email
        username = jwt.getClaimAsString("email");
        if (username != null && !username.isEmpty()) {
            return username;
        }

        // 4. Fallback на sub (UUID)
        return jwt.getSubject();
    }
}
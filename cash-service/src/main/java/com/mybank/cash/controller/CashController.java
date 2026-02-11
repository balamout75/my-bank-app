package com.mybank.cash.controller;

import org.springframework.security.oauth2.jwt.Jwt;

import com.mybank.cash.dto.CashOperationRequest;
import com.mybank.cash.dto.OperationKeyResponse;
import com.mybank.cash.model.CashOperation;
import com.mybank.cash.service.CashService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/cash")
@RequiredArgsConstructor
public class CashController {

    private final CashService cashService;

    @GetMapping("/operation-key")
    public ResponseEntity<OperationKeyResponse> getOperationKey(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(cashService.generateOperationKey(extractUsername(jwt)));
    }

    @PostMapping("/operate")
    public ResponseEntity<Void> operate(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CashOperationRequest request) {
        cashService.operate(extractUsername(jwt), request);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/operation/{operationId}")
    public ResponseEntity<CashOperation> getOperation(@PathVariable Long operationId) {
        return ResponseEntity.ok(cashService.getOperation(operationId));
    }

    private String extractUsername(Jwt jwt) {
        String username = jwt.getClaimAsString("preferred_username");
        return (username != null && !username.isEmpty()) ? username : jwt.getSubject();
    }
}
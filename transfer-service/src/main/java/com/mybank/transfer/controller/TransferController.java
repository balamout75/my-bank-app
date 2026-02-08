package com.mybank.transfer.controller;

import com.mybank.transfer.dto.OperationKeyResponse;
import com.mybank.transfer.dto.TransferOperationRequest;
import com.mybank.transfer.model.TransferOperation;
import com.mybank.transfer.service.TransferService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/transfer")
@RequiredArgsConstructor
public class TransferController {

    private final TransferService transferService;

    @GetMapping("/operation-key")
    public ResponseEntity<OperationKeyResponse> getOperationKey(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(transferService.generateOperationKey(extractUsername(jwt)));
    }

    @PostMapping("/transfer")
    public ResponseEntity<Void> operate(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody TransferOperationRequest request) {
        transferService.transfer(extractUsername(jwt), request);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/operation/{operationId}")
    public ResponseEntity<TransferOperation> getOperation(@PathVariable Long operationId) {
        return ResponseEntity.ok(transferService.getOperation(operationId));
    }

    private String extractUsername(Jwt jwt) {
        String username = jwt.getClaimAsString("preferred_username");
        return (username != null && !username.isEmpty()) ? username : jwt.getSubject();
    }
}
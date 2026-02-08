package com.mybank.notifications.controller;

import com.mybank.notifications.dto.NotificationRequest;
import com.mybank.notifications.service.NotificationService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/notifications")
@Slf4j
public class NotificationsController {

    private final NotificationService commandService;

    public NotificationsController(NotificationService commandService) {
        this.commandService = commandService;
    }

    @PostMapping
    @PreAuthorize("hasRole('notification.write')")
    public ResponseEntity<Void> notify(@Valid @RequestBody NotificationRequest req,
                                       @AuthenticationPrincipal Jwt jwt) {
        String clientId = extractClientId(jwt);
        if (commandService.createAndEnqueue(req, clientId)) {
            log.info("NOTIFY accepted: operationId={}, username={}, service={}",
                    req.operationId(), req.username(), clientId);
        } else {
            log.info("NOTIFY skipped (duplicate): operationId={}, username={}, service={}",
                    req.operationId(), req.username(), clientId);
        }
        return ResponseEntity.accepted().build();
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
	
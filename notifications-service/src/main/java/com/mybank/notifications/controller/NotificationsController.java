package com.mybank.notifications.controller;

import com.mybank.notifications.dto.NotificationRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/notifications")
@Slf4j
public class NotificationsController {

    @PostMapping
    @PreAuthorize("hasRole('notification.write')")
    public ResponseEntity<Void> notify(@RequestBody NotificationRequest req) {
        log.info("NOTIFY type={} user={} msg={} meta={}",
                req.type(), req.username(), req.message(), req.meta());
        return ResponseEntity.noContent().build();
    }
}

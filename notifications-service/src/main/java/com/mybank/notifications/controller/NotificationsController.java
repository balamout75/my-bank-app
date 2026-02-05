package com.mybank.notifications.controller;

import com.mybank.notifications.dto.NotificationRequest;
import com.mybank.notifications.service.NotificationCommandService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/notifications")
@Slf4j
public class NotificationsController {

    private final NotificationCommandService commandService;

    public NotificationsController(NotificationCommandService commandService) {
        this.commandService = commandService;
    }

    @PostMapping
    @PreAuthorize("hasRole('notification.write')")
    public ResponseEntity<Void> notify(@RequestBody NotificationRequest req) {
        var n = commandService.createAndEnqueue(req);
        log.info("NOTIFY accepted opId={} type={} user={} notificationId={}",
                req.operationId(), req.type(), req.username(), n.getId());
        return ResponseEntity.noContent().build();
    }
}
	
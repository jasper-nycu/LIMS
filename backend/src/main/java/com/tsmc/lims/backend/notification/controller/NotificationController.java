package com.tsmc.lims.backend.notification.controller;

import com.tsmc.lims.backend.notification.service.NotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users/me/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    // Only endpoint: Record stateless session termination before token clearance
    @PostMapping("/logout-log")
    public ResponseEntity<Void> logLogoutAction(Authentication auth) {
        notificationService.createNotification(
            auth.getName(),
            "Session Terminated",
            "You have safely logged out of the system.",
            "info"
        );
        return ResponseEntity.ok().build();
    }
}
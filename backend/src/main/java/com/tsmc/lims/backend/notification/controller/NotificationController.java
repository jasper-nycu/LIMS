package com.tsmc.lims.backend.notification.controller;

import com.tsmc.lims.backend.notification.dto.NotificationRequest;
import com.tsmc.lims.backend.notification.service.NotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import com.tsmc.lims.backend.notification.entity.Notification;

@RestController
@RequestMapping("/api/v1/users/me/notifications")
@PreAuthorize("isAuthenticated()")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    // Get all UNREAD notifications for the currently logged-in user.
    // "Session Terminated" is kept in the DB for audit purposes but never surfaced to the frontend.
    // Already-read notifications are excluded so they don't reappear after the user opens the tray.
    @GetMapping
    public ResponseEntity<List<Notification>> getMyNotifications(Authentication auth) {
        List<Notification> notifications = notificationService.getNotificationsByUserId(auth.getName())
                .stream()
                .filter(n -> !"Session Terminated".equals(n.getTitle()))
                .toList();
        return ResponseEntity.ok(notifications);
    }

    // Hard-delete all notifications for the current user (triggered by "Clear All" button)
    @DeleteMapping
    public ResponseEntity<Void> clearMyNotifications(Authentication auth) {
        notificationService.clearAllNotificationsByUserId(auth.getName());
        return ResponseEntity.noContent().build();
    }

    // Hard-delete a single notification by ID; only allowed if it belongs to the current user
    @DeleteMapping("/{notifId}")
    public ResponseEntity<Void> deleteOneNotification(Authentication auth, @PathVariable Integer notifId) {
        notificationService.deleteNotificationByIdAndUserId(notifId, auth.getName());
        return ResponseEntity.noContent().build();
    }

    // Mark all notifications as read for the current session state
    @PutMapping("/read")
    public ResponseEntity<Void> markAllAsRead(Authentication auth) {
        notificationService.markAllAsReadByUserId(auth.getName());
        return ResponseEntity.noContent().build();
    }
    
    // Save a client-side notification (e.g. error alerts) to DB for persistence across sessions
    @PostMapping
    public ResponseEntity<Void> createNotification(Authentication auth, @RequestBody NotificationRequest req) {
        notificationService.createNotification(auth.getName(), req.title(), req.message(), req.type());
        return ResponseEntity.ok().build();
    }

    // Record stateless session termination before token clearance
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
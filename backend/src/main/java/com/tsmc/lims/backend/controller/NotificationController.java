package com.tsmc.lims.backend.controller;

import com.tsmc.lims.backend.dto.NotificationSummary;
import com.tsmc.lims.backend.service.FabManagerService;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final FabManagerService service;

    public NotificationController(FabManagerService service) {
        this.service = service;
    }

    @GetMapping
    public List<NotificationSummary> listNotifications(@RequestParam String employeeId) {
        return service.listNotifications(employeeId);
    }

    @PostMapping("/read")
    public void markRead(@RequestParam String employeeId) {
        service.markNotificationsRead(employeeId);
    }

    @DeleteMapping("/{notificationId}")
    public void deleteNotification(@RequestParam String employeeId, @PathVariable Long notificationId) {
        service.deleteNotification(employeeId, notificationId);
    }

    @DeleteMapping
    public void clearNotifications(@RequestParam String employeeId) {
        service.clearNotifications(employeeId);
    }
}

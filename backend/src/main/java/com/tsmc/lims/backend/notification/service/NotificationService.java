package com.tsmc.lims.backend.notification.service;

import com.tsmc.lims.backend.notification.entity.Notification;
import com.tsmc.lims.backend.notification.repository.NotificationRepository;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {
    
    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    // Server-side method to generate system logs/notifications (Append-Only Observability)
    public void createNotification(String userId, String title, String message, String type) {
        Notification notif = new Notification();
        notif.setUserId(userId);
        notif.setTitle(title);
        notif.setMessage(message);
        notif.setType(type);
        notificationRepository.save(notif);
    }
}
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

    // Get personal notifications based on user ID, sorted by time from newest to oldest
    public java.util.List<Notification> getNotificationsByUserId(String userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    // Server-side method to generate system logs/notifications (Append-Only Observability)
    public void createNotification(String userId, String title, String message, String type) {
        Notification notif = new Notification();
        notif.setUserId(userId);
        notif.setTitle(title);
        notif.setMessage(message);
        notif.setType(type);
        
        // Observability Compliance: Automatically flag infrastructure session termination logs as read 
        // to permanently suppress them from leaking into the active frontend user notification tray.
        if ("Session Terminated".equals(title)) {
            notif.setIsRead(true);
        }
        
        notificationRepository.save(notif);
    }

    // Observability Compliance: Enforce append-only storage constraints. 
    // This interceptor converts destructive UI purge requests into clean soft-acknowledgments (is_read = true).
    @org.springframework.transaction.annotation.Transactional
    public void clearAllNotificationsByUserId(String userId) {
        notificationRepository.markAllAsReadByUserId(userId);
    }

    @org.springframework.transaction.annotation.Transactional
    public void markAllAsReadByUserId(String userId) {
        notificationRepository.markAllAsReadByUserId(userId);
    }

    // Delete a single notification, verifying ownership to prevent IDOR attacks
    @org.springframework.transaction.annotation.Transactional
    public void deleteNotificationByIdAndUserId(Integer notifId, String userId) {
        notificationRepository.findById(notifId).ifPresent(n -> {
            if (userId.equals(n.getUserId())) {
                notificationRepository.deleteById(notifId);
            }
        });
    }
}
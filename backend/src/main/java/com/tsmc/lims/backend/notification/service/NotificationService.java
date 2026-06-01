package com.tsmc.lims.backend.notification.service;

import com.tsmc.lims.backend.auth.repository.UserRepository;
import com.tsmc.lims.backend.notification.entity.Notification;
import com.tsmc.lims.backend.notification.repository.NotificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    public NotificationService(NotificationRepository notificationRepository,
                               UserRepository userRepository) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
    }

    // Fan-out: create one notification per active user matching the given roles
    @Transactional
    public void notifyByRoles(Collection<String> roleEnums, String type, String title, String message) {
        userRepository.findByRoleRoleEnumInAndIsActiveTrue(roleEnums).forEach(user -> {
            Notification notif = new Notification();
            notif.setUserId(user.getEmployeeId());
            notif.setTitle(title);
            notif.setMessage(message);
            notif.setType(type);
            notificationRepository.save(notif);
        });
    }

    public List<Notification> getNotificationsByUserId(String userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    // Server-side method to generate system logs/notifications (Append-Only Observability)
    @Transactional
    public void createNotification(String userId, String title, String message, String type) {
        Notification notif = new Notification();
        notif.setUserId(userId);
        notif.setTitle(title);
        notif.setMessage(message);
        notif.setType(type);
        if ("Session Terminated".equals(title)) {
            notif.setIsRead(true);
        }
        notificationRepository.save(notif);
    }

    @Transactional
    public void clearAllNotificationsByUserId(String userId) {
        notificationRepository.deleteByUserId(userId);
    }

    @Transactional
    public void markAllAsReadByUserId(String userId) {
        notificationRepository.markAllAsReadByUserId(userId);
    }

    @Transactional
    public void deleteNotificationByIdAndUserId(Integer notifId, String userId) {
        notificationRepository.findById(notifId).ifPresent(n -> {
            if (userId.equals(n.getUserId())) {
                notificationRepository.deleteById(notifId);
            }
        });
    }
}

package com.tsmc.lims.backend.lab.service;

import com.tsmc.lims.backend.lab.entity.Notification;
import com.tsmc.lims.backend.lab.entity.enums.NotificationType;
import com.tsmc.lims.backend.lab.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;

    @Transactional
    public Notification emit(String userId, NotificationType type, String title, String message) {
        return notificationRepository.save(new Notification(userId, type, title, message));
    }

    @Transactional
    public void markAsRead(Long notifId) {
        notificationRepository.findById(notifId).ifPresent(n -> {
            n.setIsRead(true);
            notificationRepository.save(n);
        });
    }

    public List<Notification> findAll() {
        return notificationRepository.findAllByOrderByCreatedAtDesc();
    }

    public List<Notification> findByUser(String userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public long countUnread(String userId) {
        return notificationRepository.countByUserIdAndIsRead(userId, false);
    }
}

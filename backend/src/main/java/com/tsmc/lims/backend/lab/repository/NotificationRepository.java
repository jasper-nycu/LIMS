package com.tsmc.lims.backend.lab.repository;

import com.tsmc.lims.backend.lab.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByUserIdOrderByCreatedAtDesc(String userId);
    List<Notification> findAllByOrderByCreatedAtDesc();
    long countByUserIdAndIsRead(String userId, boolean isRead);
}

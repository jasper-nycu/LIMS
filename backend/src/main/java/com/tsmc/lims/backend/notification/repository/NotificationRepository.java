package com.tsmc.lims.backend.notification.repository;

import com.tsmc.lims.backend.notification.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Integer> {
    // Strictly Append-Only Audit Log: No custom deletion or fetching queries needed.
}
package com.tsmc.lims.backend.repository;

import com.tsmc.lims.backend.domain.NotificationEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<NotificationEntity, Long> {
    List<NotificationEntity> findByUserEmployeeIdOrderByCreatedAtDesc(String employeeId);

    void deleteByNotifIdAndUserEmployeeId(Long notifId, String employeeId);

    void deleteByUserEmployeeId(String employeeId);
}

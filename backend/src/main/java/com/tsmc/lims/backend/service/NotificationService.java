package com.tsmc.lims.backend.service;

import com.tsmc.lims.backend.entity.Notification;
import com.tsmc.lims.backend.entity.enums.NotificationType;
import com.tsmc.lims.backend.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;

    @Transactional
    public Notification emit(String machineId, NotificationType type, String title, String description) {
        return notificationRepository.save(new Notification(machineId, type, title, description));
    }

    public List<Notification> findAll() {
        return notificationRepository.findAllByOrderByCreatedAtDesc();
    }

    public List<Notification> findByMachine(String machineId) {
        return notificationRepository.findByMachineIdOrderByCreatedAtDesc(machineId);
    }
}

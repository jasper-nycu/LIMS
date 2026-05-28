package com.tsmc.lims.backend.lab.service;

import com.tsmc.lims.backend.lab.entity.WipTask;
import com.tsmc.lims.backend.lab.entity.enums.WipStatus;
import com.tsmc.lims.backend.lab.repository.WipTaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class WipService {

    private final WipTaskRepository wipTaskRepository;

    @Transactional(readOnly = true)
    public List<WipTask> findQueue() {
        return wipTaskRepository.findByStatus(WipStatus.QUEUE);
    }

    @Transactional(readOnly = true)
    public List<WipTask> findPendingSorting() {
        return wipTaskRepository.findByStatus(WipStatus.PENDING_SORTING);
    }

    @Transactional(readOnly = true)
    public List<WipTask> findAll() {
        return wipTaskRepository.findAll();
    }
}

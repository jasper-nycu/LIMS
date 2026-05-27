package com.tsmc.lims.backend.repository;

import com.tsmc.lims.backend.entity.WipTask;
import com.tsmc.lims.backend.entity.enums.WipStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WipTaskRepository extends JpaRepository<WipTask, Long> {
    List<WipTask> findByStatus(WipStatus status);
    List<WipTask> findByStatusIn(List<WipStatus> statuses);
    List<WipTask> findByMachineIdAndStatus(String machineId, WipStatus status);
    List<WipTask> findByMachineId(String machineId);
}

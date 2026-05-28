package com.tsmc.lims.backend.lab.repository;

import com.tsmc.lims.backend.lab.entity.WipTask;
import com.tsmc.lims.backend.lab.entity.enums.WipStatus;
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

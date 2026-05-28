package com.tsmc.lims.backend.lab.repository;

import com.tsmc.lims.backend.lab.entity.MachineLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MachineLogRepository extends JpaRepository<MachineLog, Long> {
    List<MachineLog> findByMachineIdOrderByCreatedAtAsc(String machineId);
}

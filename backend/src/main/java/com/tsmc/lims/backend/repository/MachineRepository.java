package com.tsmc.lims.backend.repository;

import com.tsmc.lims.backend.entity.Machine;
import com.tsmc.lims.backend.entity.enums.MachineState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MachineRepository extends JpaRepository<Machine, String> {
    List<Machine> findByLabId(String labId);
    List<Machine> findByState(MachineState state);
}

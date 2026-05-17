package com.tsmc.lims.backend.machine;

import org.springframework.data.jpa.repository.JpaRepository;

public interface MachineRepository extends JpaRepository<MachineEntity, String> {
}

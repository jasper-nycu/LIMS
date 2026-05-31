package com.tsmc.lims.backend.fabuser.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

import com.tsmc.lims.backend.fabuser.entity.Wafer;

public interface WaferRepository extends JpaRepository<Wafer, Integer> {
    List<Wafer> findByRequestRequestIdOrderByWaferCode(String requestId);
}

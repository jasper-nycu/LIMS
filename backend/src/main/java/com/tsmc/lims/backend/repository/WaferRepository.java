package com.tsmc.lims.backend.repository;

import com.tsmc.lims.backend.domain.WaferEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WaferRepository extends JpaRepository<WaferEntity, Integer> {
    List<WaferEntity> findByRequestRequestIdOrderByWaferCode(String requestId);
}

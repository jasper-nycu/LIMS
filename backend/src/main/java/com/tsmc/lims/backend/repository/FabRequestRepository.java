package com.tsmc.lims.backend.repository;

import com.tsmc.lims.backend.domain.FabRequestEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FabRequestRepository extends JpaRepository<FabRequestEntity, String> {
    List<FabRequestEntity> findByRequesterEmployeeIdOrderByCreatedAtDesc(String requesterId);

    List<FabRequestEntity> findByStatusOrderByCreatedAtAsc(String status);
}

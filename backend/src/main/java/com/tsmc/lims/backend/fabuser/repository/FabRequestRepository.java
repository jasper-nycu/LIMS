package com.tsmc.lims.backend.fabuser.repository;

import com.tsmc.lims.backend.fabuser.domain.FabRequestEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FabRequestRepository extends JpaRepository<FabRequestEntity, String> {
    List<FabRequestEntity> findByRequesterEmployeeIdOrderByCreatedAtDesc(String requesterId);

    List<FabRequestEntity> findByStatusOrderByCreatedAtAsc(String status);
}

package com.tsmc.lims.backend.repository;

import com.tsmc.lims.backend.domain.WipTaskEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WipTaskRepository extends JpaRepository<WipTaskEntity, Integer> {
    boolean existsByRequestRequestId(String requestId);

    List<WipTaskEntity> findByStatusOrderByDispatchedAtAsc(String status);
}

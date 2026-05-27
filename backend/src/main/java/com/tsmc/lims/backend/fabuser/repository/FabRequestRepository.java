package com.tsmc.lims.backend.fabuser.repository;

import com.tsmc.lims.backend.fabuser.domain.FabRequestEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FabRequestRepository extends JpaRepository<FabRequestEntity, String> {
    List<FabRequestEntity> findByRequesterEmployeeIdOrderByCreatedAtDesc(String requesterId);

    List<FabRequestEntity> findByStatusOrderByCreatedAtAsc(String status);

    @Query("""
            select request from FabRequestEntity request
            where request.status = :status
            order by
                case request.priority
                    when 'CRITICAL' then 0
                    when 'URGENT' then 1
                    else 2
                end,
                request.createdAt asc,
                request.requestId asc
            """)
    List<FabRequestEntity> findByStatusOrderedForManagerQueue(@Param("status") String status);
}

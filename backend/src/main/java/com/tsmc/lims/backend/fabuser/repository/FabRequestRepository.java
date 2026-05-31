package com.tsmc.lims.backend.fabuser.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.tsmc.lims.backend.fabuser.entity.FabRequest;

public interface FabRequestRepository extends JpaRepository<FabRequest, String> {
    List<FabRequest> findByRequesterEmployeeIdOrderByCreatedAtDesc(String requesterId);

    List<FabRequest> findByStatusOrderByCreatedAtAsc(String status);

    @Query("""
            select request from FabRequest request
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
    List<FabRequest> findByStatusOrderedForManagerQueue(@Param("status") String status);
}

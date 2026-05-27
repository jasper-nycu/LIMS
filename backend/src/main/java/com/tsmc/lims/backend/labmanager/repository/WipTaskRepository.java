package com.tsmc.lims.backend.labmanager.repository;

import com.tsmc.lims.backend.labmanager.domain.WipTaskEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WipTaskRepository extends JpaRepository<WipTaskEntity, Integer> {
    boolean existsByRequestRequestId(String requestId);

    @Query("""
            select task from WipTaskEntity task
            join fetch task.request request
            join fetch task.experiment experiment
            where task.status = :status
            order by
                case request.priority
                    when 'CRITICAL' then 0
                    when 'URGENT' then 1
                    else 2
                end,
                task.dispatchedAt asc,
                task.taskId asc
            """)
    List<WipTaskEntity> findByStatusOrderedForSorting(@Param("status") String status);
}

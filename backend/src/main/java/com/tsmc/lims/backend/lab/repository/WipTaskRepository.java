package com.tsmc.lims.backend.lab.repository;

import com.tsmc.lims.backend.lab.entity.WipTask;
import com.tsmc.lims.backend.lab.entity.enums.WipStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface WipTaskRepository extends JpaRepository<WipTask, Long> {
    List<WipTask> findByStatus(WipStatus status);
    List<WipTask> findByStatusIn(List<WipStatus> statuses);
    List<WipTask> findByMachineIdAndStatus(String machineId, WipStatus status);
    List<WipTask> findByMachineId(String machineId);
    boolean existsByRequestRequestId(String requestId);

    @Query("""
            select task from WipTask task
            join fetch task.request request
            join fetch task.experiment experiment
            where task.status in :statuses
            order by
                case request.priority
                    when 'CRITICAL' then 0
                    when 'URGENT' then 1
                    else 2
                end,
                task.dispatchedAt asc,
                task.taskId asc
            """)
    List<WipTask> findByStatusInOrderedForSorting(@Param("statuses") Collection<WipStatus> statuses);
}

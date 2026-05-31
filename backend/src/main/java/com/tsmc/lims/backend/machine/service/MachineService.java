package com.tsmc.lims.backend.machine.service;

import com.tsmc.lims.backend.machine.dto.MachineDashboardDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class MachineService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * Retrieves all machines and calculates their current WIP loaded counts.
     * Maps the relational database result directly into DTOs.
     */
    public List<MachineDashboardDto> getMachineDashboards() {
        final String sql = "SELECT machine_id, name, capacity, state, current_utilization, error_code FROM machines";
        List<Map<String, Object>> list = jdbcTemplate.queryForList(sql);

        // Attach loaded (processing) counts for each machine
        final String countSql = "SELECT COUNT(*) FROM wip_tasks WHERE machine_id = ? AND status = 'PROCESSING'";

        return list.stream().map(r -> {
            String machineId = (String) r.get("machine_id");
            Integer count = jdbcTemplate.queryForObject(countSql, Integer.class, machineId);
            
            // Map directly to the immutable record
            return new MachineDashboardDto(
                machineId,
                (String) r.get("name"),
                (Integer) r.get("capacity"),
                (String) r.get("state"),
                (Integer) r.get("current_utilization"),
                count == null ? 0 : count,
                (String) r.get("error_code")
            );
        }).collect(Collectors.toList());
    }
}
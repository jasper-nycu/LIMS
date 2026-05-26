package com.tsmc.lims.backend.machine.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/machines")
public class MachineController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @GetMapping
    public List<Map<String, Object>> getMachines() {
        final String sql = "SELECT machine_id, name, capacity, state, current_utilization, error_code FROM machines";
        List<Map<String, Object>> list = jdbcTemplate.queryForList(sql);

        // Attach loaded (processing) counts for each machine
        final String countSql = "SELECT COUNT(*) FROM wip_tasks WHERE machine_id = ? AND status = 'PROCESSING'";

        return list.stream().map(r -> {
            Map<String, Object> dto = new HashMap<>(r);
            Integer count = jdbcTemplate.queryForObject(countSql, Integer.class, r.get("machine_id"));
            dto.put("loadedCount", count == null ? 0 : count);
            // Normalize keys to simpler names used in frontend
            dto.put("id", r.get("machine_id"));
            dto.put("cap", r.get("capacity"));
            dto.put("currentUtil", r.get("current_utilization"));
            dto.put("error", r.get("error_code"));
            dto.remove("machine_id");
            dto.remove("capacity");
            dto.remove("current_utilization");
            dto.remove("error_code");
            return dto;
        }).collect(Collectors.toList());
    }
}

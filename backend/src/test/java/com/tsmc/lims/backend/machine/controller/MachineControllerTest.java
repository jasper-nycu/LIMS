package com.tsmc.lims.backend.machine.controller;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MachineControllerTest {

    @Test
    void getMachinesReturnsCapacityAnalyticsContract() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        MachineController controller = new MachineController();
        ReflectionTestUtils.setField(controller, "jdbcTemplate", jdbcTemplate);

        Map<String, Object> machine = new HashMap<>();
        machine.put("machine_id", "SEM-01");
        machine.put("name", "Surface Scan (SEM)");
        machine.put("capacity", 25);
        machine.put("state", "PROCESSING");
        machine.put("current_utilization", 72);
        machine.put("error_code", null);

        when(jdbcTemplate.queryForList("SELECT machine_id, name, capacity, state, current_utilization, error_code FROM machines"))
                .thenReturn(List.of(machine));
        when(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM wip_tasks WHERE machine_id = ? AND status = 'PROCESSING'",
                Integer.class,
                "SEM-01"
        )).thenReturn(18);

        List<Map<String, Object>> response = controller.getMachines();

        assertThat(response).hasSize(1);
        assertThat(response.get(0))
                .containsEntry("id", "SEM-01")
                .containsEntry("name", "Surface Scan (SEM)")
                .containsEntry("cap", 25)
                .containsEntry("currentUtil", 72)
                .containsEntry("loadedCount", 18)
                .containsEntry("error", null);
        assertThat(response.get(0)).doesNotContainKeys("machine_id", "capacity", "current_utilization", "error_code");
    }
}

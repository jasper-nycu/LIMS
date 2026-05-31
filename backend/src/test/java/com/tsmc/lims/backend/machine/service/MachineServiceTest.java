package com.tsmc.lims.backend.machine.service;

import com.tsmc.lims.backend.machine.dto.MachineDashboardDto;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MachineServiceTest {

    @Test
    void getMachineDashboardsReturnsCapacityAnalyticsContract() {
        // Arrange
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        MachineService service = new MachineService();
        ReflectionTestUtils.setField(service, "jdbcTemplate", jdbcTemplate);

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

        // Act
        List<MachineDashboardDto> response = service.getMachineDashboards();

        // Assert
        assertThat(response).hasSize(1);
        assertThat(response.get(0).id()).isEqualTo("SEM-01");
        assertThat(response.get(0).name()).isEqualTo("Surface Scan (SEM)");
        assertThat(response.get(0).cap()).isEqualTo(25);
        assertThat(response.get(0).currentUtil()).isEqualTo(72);
        assertThat(response.get(0).loadedCount()).isEqualTo(18);
    }
}
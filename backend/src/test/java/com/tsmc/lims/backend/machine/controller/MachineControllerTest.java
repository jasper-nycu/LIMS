package com.tsmc.lims.backend.machine.controller;

import com.tsmc.lims.backend.machine.dto.MachineDashboardDto;
import com.tsmc.lims.backend.machine.service.MachineService;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MachineControllerTest {

    @Test
    void getMachinesDelegatesToMachineService() {
        // Arrange
        MachineService machineService = mock(MachineService.class);
        MachineController controller = new MachineController();
        ReflectionTestUtils.setField(controller, "machineService", machineService);

        // Prepare a mock DTO response
        MachineDashboardDto mockDto = new MachineDashboardDto(
                "SEM-01", "Surface Scan (SEM)", 25, "PROCESSING", 72, 18, null
        );
        when(machineService.getMachineDashboards()).thenReturn(List.of(mockDto));

        // Act
        List<MachineDashboardDto> response = controller.getMachines();

        // Assert
        assertThat(response).hasSize(1);
        assertThat(response.get(0).id()).isEqualTo("SEM-01");
        assertThat(response.get(0).name()).isEqualTo("Surface Scan (SEM)");
        assertThat(response.get(0).cap()).isEqualTo(25);
        assertThat(response.get(0).state()).isEqualTo("PROCESSING");
        assertThat(response.get(0).currentUtil()).isEqualTo(72);
        assertThat(response.get(0).loadedCount()).isEqualTo(18);
        assertThat(response.get(0).error()).isNull();
    }
}
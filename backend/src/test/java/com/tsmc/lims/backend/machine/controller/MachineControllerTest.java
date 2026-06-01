package com.tsmc.lims.backend.machine.controller;

import com.tsmc.lims.backend.lab.controller.MachineController;
import com.tsmc.lims.backend.lab.dto.MachineResponse;
import com.tsmc.lims.backend.lab.entity.enums.MachineState;
import com.tsmc.lims.backend.lab.service.MachineLogService;
import com.tsmc.lims.backend.lab.service.MachineService;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MachineControllerTest {

    @Test
    void getAll_delegatesToMachineServiceAndReturnsRawList() {
        MachineService machineService = mock(MachineService.class);
        MachineLogService machineLogService = mock(MachineLogService.class);
        MachineController controller = new MachineController(machineService, machineLogService);

        MachineResponse mockResponse = new MachineResponse(
                "SEM-01", "Surface Scan (SEM)", "exp_sem",
                MachineState.PROCESSING, 25, 18,
                List.of("W-1001", "W-1002"), null, 72,
                List.of("MW", "JS")
        );
        when(machineService.findAll()).thenReturn(List.of(mockResponse));

        ResponseEntity<List<MachineResponse>> response = controller.getAll();

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).hasSize(1);
        MachineResponse m = response.getBody().get(0);
        assertThat(m.getId()).isEqualTo("SEM-01");
        assertThat(m.getName()).isEqualTo("Surface Scan (SEM)");
        assertThat(m.getCap()).isEqualTo(25);
        assertThat(m.getState()).isEqualTo(MachineState.PROCESSING);
        assertThat(m.getCurrentUtil()).isEqualTo(72);
        assertThat(m.getLoadedCount()).isEqualTo(18);
        assertThat(m.getOwners()).containsExactly("MW", "JS");
        assertThat(m.getError()).isNull();
    }
}

package com.tsmc.lims.backend.labmanager.controller;

import com.tsmc.lims.backend.labmanager.dto.LabWipSummary;
import com.tsmc.lims.backend.labmanager.service.LabManagerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class LabOperationsControllerTest {

    private LabManagerService service;
    private LabOperationsController controller;

    @BeforeEach
    void setUp() {
        service = mock(LabManagerService.class);
        controller = new LabOperationsController(service);
    }

    // ── GET /lab/wips ─────────────────────────────────────────────────────────

    @Test
    void listPendingWips_delegatesToServiceAndReturnsList() {
        LabWipSummary wip = new LabWipSummary("W-1234-exp_sem-1", "W-1234", "exp_sem", "CRITICAL");
        when(service.listPendingWips()).thenReturn(List.of(wip));

        List<LabWipSummary> result = controller.listPendingWips();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).waferCode()).isEqualTo("W-1234");
        assertThat(result.get(0).priority()).isEqualTo("CRITICAL");
        verify(service).listPendingWips();
    }

    @Test
    void listPendingWips_returnsEmptyListWhenNoWips() {
        when(service.listPendingWips()).thenReturn(List.of());

        assertThat(controller.listPendingWips()).isEmpty();
    }
}

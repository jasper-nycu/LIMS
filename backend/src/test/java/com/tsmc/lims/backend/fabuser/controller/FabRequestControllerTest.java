package com.tsmc.lims.backend.fabuser.controller;

import com.tsmc.lims.backend.fabuser.dto.CreateFabRequest;
import com.tsmc.lims.backend.fabuser.dto.ExperimentOption;
import com.tsmc.lims.backend.fabuser.dto.FabRequestSummary;
import com.tsmc.lims.backend.fabuser.dto.LaboratoryOption;
import com.tsmc.lims.backend.fabuser.service.FabRequestService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class FabRequestControllerTest {

    private FabRequestService service;
    private FabRequestController controller;

    @BeforeEach
    void setUp() {
        service = mock(FabRequestService.class);
        controller = new FabRequestController(service);
    }

    // ── GET /fab/labs ─────────────────────────────────────────────────────────

    @Test
    void listLabs_delegatesToServiceAndReturnsList() {
        LaboratoryOption lab = new LaboratoryOption("LAB_MA", "Material Analysis",
                List.of(new ExperimentOption("exp_sem", "SEM")));
        when(service.listLaboratories()).thenReturn(List.of(lab));

        List<LaboratoryOption> result = controller.listLabs();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).labId()).isEqualTo("LAB_MA");
        assertThat(result.get(0).labName()).isEqualTo("Material Analysis");
        assertThat(result.get(0).experiments()).hasSize(1);
        verify(service).listLaboratories();
    }

    @Test
    void listLabs_returnsEmptyListWhenNoLabs() {
        when(service.listLaboratories()).thenReturn(List.of());

        assertThat(controller.listLabs()).isEmpty();
    }

    // ── POST /fab/requests ────────────────────────────────────────────────────

    @Test
    void createRequest_delegatesToServiceAndReturnsSummary() {
        CreateFabRequest req = new CreateFabRequest(
                "TS-1234", "LAB_MA", List.of("exp_sem"), List.of("W-1234"), "NORMAL", "Test remark");
        FabRequestSummary summary = new FabRequestSummary(
                "REQ-100001", "TS-1234", "LAB_MA",
                List.of("W-1234"), List.of("SEM"), 1, "PENDING", "NORMAL", "Test remark", null, null);
        when(service.createRequest(req)).thenReturn(summary);

        FabRequestSummary result = controller.createRequest(req);

        assertThat(result.id()).isEqualTo("REQ-100001");
        assertThat(result.status()).isEqualTo("PENDING");
        assertThat(result.waferCount()).isEqualTo(1);
        verify(service).createRequest(req);
    }

    // ── GET /fab/requests ─────────────────────────────────────────────────────

    @Test
    void listRequests_delegatesToServiceWithRequesterId() {
        FabRequestSummary s1 = new FabRequestSummary(
                "REQ-100001", "TS-1234", "LAB_MA",
                List.of("W-1234"), List.of("SEM"), 1, "PENDING", "NORMAL", "", null, null);
        FabRequestSummary s2 = new FabRequestSummary(
                "REQ-100002", "TS-1234", "LAB_MA",
                List.of("W-5678"), List.of("XRD"), 1, "APPROVED", "URGENT", "", null, null);
        when(service.listRequestsByRequester("TS-1234")).thenReturn(List.of(s1, s2));

        List<FabRequestSummary> result = controller.listRequests("TS-1234");

        assertThat(result).hasSize(2);
        assertThat(result.get(0).requesterId()).isEqualTo("TS-1234");
        assertThat(result.get(1).priority()).isEqualTo("URGENT");
        verify(service).listRequestsByRequester("TS-1234");
    }
}

package com.tsmc.lims.backend.labmanager.controller;

import com.tsmc.lims.backend.labmanager.dto.DecisionRequest;
import com.tsmc.lims.backend.labmanager.dto.ManagerRequestSummary;
import com.tsmc.lims.backend.labmanager.service.LabManagerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class LabManagerControllerTest {

    private LabManagerService service;
    private LabManagerController controller;

    @BeforeEach
    void setUp() {
        service = mock(LabManagerService.class);
        controller = new LabManagerController(service);
    }

    // ── GET /manager/requests/pending ─────────────────────────────────────────

    @Test
    void listPendingRequests_delegatesToServiceAndReturnsList() {
        ManagerRequestSummary summary = new ManagerRequestSummary(
                "REQ-100001", "John Doe", "FAB_USER",
                List.of("W-1234"), List.of("exp_sem"), "Test remark", "NORMAL", "2025-06-01 10:00");
        when(service.listPendingRequests()).thenReturn(List.of(summary));

        List<ManagerRequestSummary> result = controller.listPendingRequests();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo("REQ-100001");
        assertThat(result.get(0).requester()).isEqualTo("John Doe");
        verify(service).listPendingRequests();
    }

    @Test
    void listPendingRequests_returnsEmptyListWhenNoPendingRequests() {
        when(service.listPendingRequests()).thenReturn(List.of());

        assertThat(controller.listPendingRequests()).isEmpty();
    }

    // ── POST /manager/requests/{requestId}/approve ────────────────────────────

    @Test
    void approve_delegatesToServiceWithApproverIdFromBody() {
        DecisionRequest decision = new DecisionRequest("TS-9001", null);
        ManagerRequestSummary summary = new ManagerRequestSummary(
                "REQ-100001", "John Doe", "FAB_USER",
                List.of("W-1234"), List.of("exp_sem"), "Test", "NORMAL", "2025-06-01 10:00");
        when(service.approveRequest("REQ-100001", "TS-9001")).thenReturn(summary);

        ManagerRequestSummary result = controller.approve("REQ-100001", decision);

        assertThat(result.id()).isEqualTo("REQ-100001");
        verify(service).approveRequest("REQ-100001", "TS-9001");
    }

    @Test
    void approve_nullDecisionBody_passesNullApproverIdToService() {
        when(service.approveRequest("REQ-100001", null))
                .thenReturn(new ManagerRequestSummary("REQ-100001", "System", "SYSTEM",
                        List.of(), List.of(), "", "NORMAL", ""));

        controller.approve("REQ-100001", null);

        verify(service).approveRequest("REQ-100001", null);
    }

    // ── POST /manager/requests/{requestId}/reject ─────────────────────────────

    @Test
    void reject_delegatesToServiceWithAllDecisionFields() {
        DecisionRequest decision = new DecisionRequest("TS-9001", "Experiment not feasible.");
        ManagerRequestSummary summary = new ManagerRequestSummary(
                "REQ-100001", "John Doe", "FAB_USER",
                List.of("W-1234"), List.of("exp_sem"), "Test", "NORMAL", "2025-06-01 10:00");
        when(service.rejectRequest("REQ-100001", "TS-9001", "Experiment not feasible."))
                .thenReturn(summary);

        ManagerRequestSummary result = controller.reject("REQ-100001", decision);

        assertThat(result.id()).isEqualTo("REQ-100001");
        verify(service).rejectRequest("REQ-100001", "TS-9001", "Experiment not feasible.");
    }
}


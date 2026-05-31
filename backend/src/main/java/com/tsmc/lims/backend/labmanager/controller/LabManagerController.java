package com.tsmc.lims.backend.labmanager.controller;

import com.tsmc.lims.backend.labmanager.dto.DecisionRequest;
import com.tsmc.lims.backend.labmanager.dto.ManagerRequestSummary;
import com.tsmc.lims.backend.labmanager.service.LabManagerService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/manager")
@PreAuthorize("hasAnyRole('SYSADMIN', 'LAB_MANAGER', 'MACHINE_OWNER')")
public class LabManagerController {

    private final LabManagerService service;

    public LabManagerController(LabManagerService service) {
        this.service = service;
    }

    @GetMapping("/requests/pending")
    public List<ManagerRequestSummary> listPendingRequests() {
        return service.listPendingRequests();
    }

    @PostMapping("/requests/{requestId}/approve")
    public ManagerRequestSummary approve(@PathVariable String requestId, @RequestBody(required = false) DecisionRequest decision) {
        String approverId = decision == null ? null : decision.approverId();
        return service.approveRequest(requestId, approverId);
    }

    @PostMapping("/requests/{requestId}/reject")
    public ManagerRequestSummary reject(@PathVariable String requestId, @RequestBody DecisionRequest decision) {
        String approverId = decision == null ? null : decision.approverId();
        String rejectReason = decision == null ? null : decision.rejectReason();
        return service.rejectRequest(requestId, approverId, rejectReason);
    }
}
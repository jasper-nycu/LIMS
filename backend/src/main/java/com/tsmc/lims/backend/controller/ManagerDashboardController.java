package com.tsmc.lims.backend.controller;

import com.tsmc.lims.backend.dto.DecisionRequest;
import com.tsmc.lims.backend.dto.ManagerRequestSummary;
import com.tsmc.lims.backend.service.FabManagerService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/manager")
public class ManagerDashboardController {

    private final FabManagerService service;

    public ManagerDashboardController(FabManagerService service) {
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

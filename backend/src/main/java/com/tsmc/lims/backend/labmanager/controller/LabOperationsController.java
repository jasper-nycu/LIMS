package com.tsmc.lims.backend.labmanager.controller;

import com.tsmc.lims.backend.labmanager.dto.LabWipSummary;
import com.tsmc.lims.backend.labmanager.service.LabManagerService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/lab")
@PreAuthorize("hasAnyRole('SYSADMIN', 'LAB_MANAGER', 'LAB_OPERATOR')")
public class LabOperationsController {

    private final LabManagerService service;

    public LabOperationsController(LabManagerService service) {
        this.service = service;
    }

    @GetMapping("/wips")
    public List<LabWipSummary> listPendingWips() {
        return service.listPendingWips();
    }
}
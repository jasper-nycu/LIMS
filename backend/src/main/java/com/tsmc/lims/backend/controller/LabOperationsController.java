package com.tsmc.lims.backend.controller;

import com.tsmc.lims.backend.dto.LabWipSummary;
import com.tsmc.lims.backend.service.FabManagerService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/lab")
public class LabOperationsController {

    private final FabManagerService service;

    public LabOperationsController(FabManagerService service) {
        this.service = service;
    }

    @GetMapping("/wips")
    public List<LabWipSummary> listPendingWips() {
        return service.listPendingWips();
    }
}

package com.tsmc.lims.backend.machine.controller;

import com.tsmc.lims.backend.machine.dto.MachineDashboardDto;
import com.tsmc.lims.backend.machine.service.MachineService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/machines")
public class MachineController {

    @Autowired
    private MachineService machineService;

    // Endpoint now delegates business logic entirely to the Service layer
    @GetMapping
    public List<MachineDashboardDto> getMachines() {
        return machineService.getMachineDashboards();
    }
}
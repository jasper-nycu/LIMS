package com.tsmc.lims.backend.lab.config;

import com.tsmc.lims.backend.lab.repository.MachineLogRepository;
import com.tsmc.lims.backend.lab.service.MachineLogService;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!test")
public class DataInitializer {

    private final MachineLogRepository machineLogRepository;
    private final MachineLogService machineLogService;

    public DataInitializer(MachineLogRepository machineLogRepository,
                           MachineLogService machineLogService) {
        this.machineLogRepository = machineLogRepository;
        this.machineLogService = machineLogService;
    }

    @PostConstruct
    public void seed() {
        if (machineLogRepository.count() > 0) return;

        seedLogs("SEM-01",       "SEM-Surface-Std");
        seedLogs("E-TEST-02",    "E-TEST-Parametric");
        machineLogService.write("BAKE-OVEN-01", "SYS", "Machine initialized");
        machineLogService.write("TEM-01",       "SYS", "Machine initialized");
        machineLogService.write("FIB-01",       "SYS", "Machine initialized");
        machineLogService.write("XRD-01",       "SYS", "Machine initialized");
    }

    private void seedLogs(String machineId, String recipe) {
        machineLogService.write(machineId, "SYS", "Machine initialized");
        machineLogService.write(machineId, "CFG", "Loading recipe: " + recipe);
        machineLogService.write(machineId, "OPS", "Machine ready for dispatch");
    }
}

package com.tsmc.lims.backend.config;

import com.tsmc.lims.backend.entity.Machine;
import com.tsmc.lims.backend.entity.Recipe;
import com.tsmc.lims.backend.entity.enums.MachineState;
import com.tsmc.lims.backend.repository.MachineRepository;
import com.tsmc.lims.backend.repository.RecipeRepository;
import com.tsmc.lims.backend.service.MachineLogService;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Profile("!test")
public class DataInitializer {

    private final MachineRepository machineRepository;
    private final RecipeRepository recipeRepository;
    private final MachineLogService machineLogService;

    public DataInitializer(MachineRepository machineRepository,
                           RecipeRepository recipeRepository,
                           MachineLogService machineLogService) {
        this.machineRepository = machineRepository;
        this.recipeRepository = recipeRepository;
        this.machineLogService = machineLogService;
    }

    @PostConstruct
    public void seed() {
        if (machineRepository.existsById("SEM-01")) return;

        Machine sem01  = machine("SEM-01",       "LAB_MA", "Surface Scan (SEM)",  "exp_sem",   MachineState.PROCESSING, 25, List.of("MW", "JS"));
        Machine bake   = machine("BAKE-OVEN-01", "LAB_RA", "High-Temp Bake",      "exp_bake",  MachineState.IDLE,       50, List.of("SC"));
        Machine tem    = machine("TEM-01",        "LAB_MA", "Deep Analysis",       "exp_deep",  MachineState.IDLE,       10, List.of("RK"));
        Machine fib    = machine("FIB-01",        "LAB_FA", "Focused Ion Beam",    "exp_fib",   MachineState.IDLE,       1,  List.of("CH"));
        Machine etest  = machine("E-TEST-02",     "LAB_RA", "Electrical Test",     "exp_etest", MachineState.PROCESSING, 50, List.of("AS"));
        Machine xrd    = machine("XRD-01",        "LAB_MA", "X-Ray Diffraction",   "exp_xrd",   MachineState.IDLE,       25, List.of("TH"));

        machineRepository.saveAll(List.of(sem01, bake, tem, fib, etest, xrd));

        recipeRepository.saveAll(List.of(
            new Recipe("SEM-Surface-Std",   "SEM-Surface-Std",   sem01),
            new Recipe("SEM-High-Res",      "SEM-High-Res",      sem01),
            new Recipe("Bake-150C-4H",      "Bake-150C-4H",      bake),
            new Recipe("Bake-250C-2H",      "Bake-250C-2H",      bake),
            new Recipe("TEM-Lattice-View",  "TEM-Lattice-View",  tem),
            new Recipe("FIB-Cross-Section", "FIB-Cross-Section", fib),
            new Recipe("FIB-Circuit-Edit",  "FIB-Circuit-Edit",  fib),
            new Recipe("E-TEST-Parametric", "E-TEST-Parametric", etest),
            new Recipe("E-TEST-Yield",      "E-TEST-Yield",      etest),
            new Recipe("XRD-Crystal-Scan",  "XRD-Crystal-Scan",  xrd)
        ));

        seedLogs(sem01.getMachineId(), "SEM-Surface-Std");
        seedLogs(etest.getMachineId(), "E-TEST-Parametric");
        machineLogService.write(bake.getMachineId(),  "SYS", "Machine initialized");
        machineLogService.write(tem.getMachineId(),   "SYS", "Machine initialized");
        machineLogService.write(fib.getMachineId(),   "SYS", "Machine initialized");
        machineLogService.write(xrd.getMachineId(),   "SYS", "Machine initialized");
    }

    private void seedLogs(String machineId, String recipe) {
        machineLogService.write(machineId, "SYS", "Machine initialized");
        machineLogService.write(machineId, "CFG", "Loading recipe: " + recipe);
        machineLogService.write(machineId, "OPS", "Machine transitioned to PROCESSING");
    }

    private Machine machine(String id, String labId, String name, String expKey,
                            MachineState state, int capacity, List<String> owners) {
        Machine m = new Machine();
        m.setMachineId(id);
        m.setLabId(labId);
        m.setName(name);
        m.setExpKey(expKey);
        m.setState(state);
        m.setCapacity(capacity);
        m.getOwners().addAll(owners);
        return m;
    }
}

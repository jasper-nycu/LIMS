package com.tsmc.lims.backend.config;

import com.tsmc.lims.backend.entity.Machine;
import com.tsmc.lims.backend.entity.Recipe;
import com.tsmc.lims.backend.entity.enums.MachineState;
import com.tsmc.lims.backend.repository.MachineRepository;
import com.tsmc.lims.backend.repository.RecipeRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Profile("!test")
public class DataInitializer {

    private final MachineRepository machineRepository;
    private final RecipeRepository recipeRepository;

    public DataInitializer(MachineRepository machineRepository, RecipeRepository recipeRepository) {
        this.machineRepository = machineRepository;
        this.recipeRepository = recipeRepository;
    }

    @PostConstruct
    public void seed() {
        if (machineRepository.existsById("SEM-01")) return;

        Machine sem01  = machine("SEM-01",       "LAB_MA", "Surface Scan (SEM)",  "exp_sem",   MachineState.PROCESSING, 25);
        Machine bake   = machine("BAKE-OVEN-01", "LAB_RA", "High-Temp Bake",      "exp_bake",  MachineState.IDLE,       50);
        Machine tem    = machine("TEM-01",        "LAB_MA", "Deep Analysis",       "exp_deep",  MachineState.IDLE,       10);
        Machine fib    = machine("FIB-01",        "LAB_FA", "Focused Ion Beam",    "exp_fib",   MachineState.IDLE,       1);
        Machine etest  = machine("E-TEST-02",     "LAB_RA", "Electrical Test",     "exp_etest", MachineState.PROCESSING, 50);
        Machine xrd    = machine("XRD-01",        "LAB_MA", "X-Ray Diffraction",   "exp_xrd",   MachineState.IDLE,       25);

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
    }

    private Machine machine(String id, String labId, String name, String expKey,
                            MachineState state, int capacity) {
        Machine m = new Machine();
        m.setMachineId(id);
        m.setLabId(labId);
        m.setName(name);
        m.setExpKey(expKey);
        m.setState(state);
        m.setCapacity(capacity);
        return m;
    }
}

package com.tsmc.lims.backend.machine;

import jakarta.annotation.PostConstruct;
import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

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
        if (!machineRepository.existsById("SEM-01")) {
            MachineEntity sem01 = new MachineEntity("SEM-01", "LAB_MA", "Surface Scan (SEM)", "exp_sem", MachineState.PROCESSING, 25, 18, null, 72, List.of("MW", "JS"));
            MachineEntity bake = new MachineEntity("BAKE-OVEN-01", "LAB_RA", "High-Temp Bake", "exp_bake", MachineState.IDLE, 50, 0, null, 0, List.of("SC"));
            MachineEntity tem = new MachineEntity("TEM-01", "LAB_MA", "Deep Analysis", "exp_deep", MachineState.IDLE, 10, 0, null, 0, List.of("RK"));
            MachineEntity fib = new MachineEntity("FIB-01", "LAB_FA", "Focused Ion Beam", "exp_fib", MachineState.IDLE, 1, 0, null, 0, List.of("CH"));
            MachineEntity etest = new MachineEntity("E-TEST-02", "LAB_RA", "Electrical Test", "exp_etest", MachineState.PROCESSING, 50, 42, null, 84, List.of("AS"));
            MachineEntity xrd = new MachineEntity("XRD-01", "LAB_MA", "X-Ray Diffraction", "exp_xrd", MachineState.IDLE, 25, 0, null, 0, List.of("TH"));

            machineRepository.saveAll(List.of(sem01, bake, tem, fib, etest, xrd));

            recipeRepository.save(new RecipeEntity("SEM-Surface-Std", "SEM-Surface-Std", sem01));
            recipeRepository.save(new RecipeEntity("SEM-High-Res", "SEM-High-Res", sem01));
            recipeRepository.save(new RecipeEntity("Bake-150C-4H", "Bake-150C-4H", bake));
            recipeRepository.save(new RecipeEntity("Bake-250C-2H", "Bake-250C-2H", bake));
            recipeRepository.save(new RecipeEntity("TEM-Lattice-View", "TEM-Lattice-View", tem));
            recipeRepository.save(new RecipeEntity("FIB-Cross-Section", "FIB-Cross-Section", fib));
            recipeRepository.save(new RecipeEntity("FIB-Circuit-Edit", "FIB-Circuit-Edit", fib));
            recipeRepository.save(new RecipeEntity("E-TEST-Parametric", "E-TEST-Parametric", etest));
            recipeRepository.save(new RecipeEntity("E-TEST-Yield", "E-TEST-Yield", etest));
            recipeRepository.save(new RecipeEntity("XRD-Crystal-Scan", "XRD-Crystal-Scan", xrd));
        }
    }
}

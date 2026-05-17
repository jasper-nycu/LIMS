package com.tsmc.lims.backend.machine;

import com.tsmc.lims.backend.machine.dto.DispatchRequest;
import com.tsmc.lims.backend.machine.dto.MachineResponse;
import com.tsmc.lims.backend.machine.dto.NameRequest;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class MachineService {

    private final MachineRepository machineRepository;
    private final RecipeRepository recipeRepository;

    public MachineService(MachineRepository machineRepository, RecipeRepository recipeRepository) {
        this.machineRepository = machineRepository;
        this.recipeRepository = recipeRepository;
    }

    public List<MachineResponse> listMachines() {
        return machineRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public MachineResponse getMachine(String id) {
        return machineRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new MachineActionException("Machine not found: " + id));
    }

    public MachineResponse dispatch(String machineId, DispatchRequest request) {
        MachineEntity machine = machineRepository.findById(machineId)
                .orElseThrow(() -> new MachineActionException("Machine not found: " + machineId));

        if (request.getWaferIds() == null || request.getWaferIds().isEmpty()) {
            throw new MachineActionException("No wafers provided for dispatch.");
        }
        if (machine.getState() != MachineState.IDLE) {
            throw new MachineActionException("Cannot dispatch unless machine is IDLE.");
        }

        int dispatchCount = request.getWaferIds().size();
        if (machine.getLoadedCount() + dispatchCount > machine.getCap()) {
            throw new MachineActionException("Total wafers exceed machine capacity.");
        }

        machine.setLoadedCount(machine.getLoadedCount() + dispatchCount);
        machine.setState(MachineState.PROCESSING);
        machine.setCurrentUtil((int) Math.round(((double) machine.getLoadedCount() / machine.getCap()) * 100));
        machine.setError(null);
        machineRepository.save(machine);
        return toResponse(machine);
    }

    public MachineResponse toggleAlarm(String machineId) {
        MachineEntity machine = machineRepository.findById(machineId)
                .orElseThrow(() -> new MachineActionException("Machine not found: " + machineId));

        if (machine.getState() == MachineState.ALARM) {
            MachineState next = machine.getLoadedCount() > 0 ? MachineState.PROCESSING : MachineState.IDLE;
            machine.setState(next);
            machine.setError(null);
            machine.setCurrentUtil(next == MachineState.PROCESSING ? (int) Math.round(((double) machine.getLoadedCount() / machine.getCap()) * 100) : 0);
        } else if (machine.getState() == MachineState.PROCESSING) {
            machine.setState(MachineState.ALARM);
            machine.setError("ERR_SIMULATED_FAULT");
            machine.setCurrentUtil(0);
        } else {
            throw new MachineActionException("Machine must be PROCESSING to enter or exit ALARM.");
        }

        machineRepository.save(machine);
        return toResponse(machine);
    }

    public MachineResponse toggleMaintenance(String machineId) {
        MachineEntity machine = machineRepository.findById(machineId)
                .orElseThrow(() -> new MachineActionException("Machine not found: " + machineId));

        if (machine.getState() == MachineState.PROCESSING) {
            throw new MachineActionException("Cannot enter maintenance while processing.");
        }

        if (machine.getState() == MachineState.MAINTENANCE) {
            MachineState next = machine.getLoadedCount() > 0 ? MachineState.PROCESSING : MachineState.IDLE;
            machine.setState(next);
            machine.setCurrentUtil(next == MachineState.PROCESSING ? (int) Math.round(((double) machine.getLoadedCount() / machine.getCap()) * 100) : 0);
        } else {
            machine.setState(MachineState.MAINTENANCE);
            machine.setError(null);
            machine.setCurrentUtil(0);
        }

        machineRepository.save(machine);
        return toResponse(machine);
    }

    public List<String> getRecipes(String machineId) {
        if (!machineRepository.existsById(machineId)) {
            throw new MachineActionException("Machine not found: " + machineId);
        }
        return recipeRepository.findByMachineId(machineId).stream()
                .map(RecipeEntity::getName)
                .collect(Collectors.toList());
    }

    public List<String> addRecipe(String machineId, NameRequest request) {
        MachineEntity machine = machineRepository.findById(machineId)
                .orElseThrow(() -> new MachineActionException("Machine not found: " + machineId));

        if (request.getName() == null || request.getName().trim().isEmpty()) {
            throw new MachineActionException("Recipe name cannot be empty.");
        }

        String recipeName = request.getName().trim();
        RecipeEntity recipe = new RecipeEntity(recipeName, recipeName, machine);
        recipeRepository.save(recipe);
        return getRecipes(machineId);
    }

    private MachineResponse toResponse(MachineEntity entity) {
        return new MachineResponse(
                entity.getId(),
                entity.getName(),
                entity.getExpKey(),
                entity.getState(),
                entity.getCap(),
                entity.getLoadedCount(),
                entity.getError(),
                entity.getCurrentUtil(),
                entity.getOwners()
        );
    }
}

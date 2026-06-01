package com.tsmc.lims.backend.lab.service;

import com.tsmc.lims.backend.lab.dto.DispatchRequest;
import com.tsmc.lims.backend.lab.dto.MachineResponse;
import com.tsmc.lims.backend.lab.dto.NameRequest;
import com.tsmc.lims.backend.lab.entity.Machine;
import com.tsmc.lims.backend.lab.entity.Recipe;
import com.tsmc.lims.backend.lab.entity.WipTask;
import com.tsmc.lims.backend.lab.entity.enums.MachineState;
import com.tsmc.lims.backend.lab.entity.enums.WipStatus;
import com.tsmc.lims.backend.lab.exception.InvalidStateTransitionException;
import com.tsmc.lims.backend.lab.exception.MachineActionException;
import com.tsmc.lims.backend.lab.exception.ResourceNotFoundException;
import com.tsmc.lims.backend.lab.repository.MachineRepository;
import com.tsmc.lims.backend.lab.repository.RecipeRepository;
import com.tsmc.lims.backend.lab.repository.WipTaskRepository;
import com.tsmc.lims.backend.fabuser.repository.FabRequestRepository;
import com.tsmc.lims.backend.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MachineService {

    // Lab staff roles — FAB User is notified individually per request, not by broadcast
    private static final Set<String> WAFER_ROLES  = Set.of("ROLE_LAB_MANAGER", "ROLE_LAB_OPERATOR");
    private static final Set<String> MACHINE_ROLES = Set.of("ROLE_MACHINE_OWNER", "ROLE_LAB_OPERATOR");

    private final MachineRepository machineRepository;
    private final WipTaskRepository wipTaskRepository;
    private final RecipeRepository recipeRepository;
    private final NotificationService notificationService;
    private final MachineLogService machineLogService;
    private final FabRequestRepository fabRequestRepository;

    // Notify the specific FAB User(s) who submitted the requests for the given tasks
    private void notifyRequesters(Collection<WipTask> tasks, String title, String message, String type) {
        tasks.stream()
            .map(WipTask::getRequestId)
            .filter(Objects::nonNull)
            .distinct()
            .forEach(reqId ->
                fabRequestRepository.findById(reqId).ifPresent(req -> {
                    if (req.getRequester() != null) {
                        notificationService.createNotification(
                            req.getRequester().getEmployeeId(), title, message, type);
                    }
                })
            );
    }

    // ── Queries ──────────────────────────────────────────────────────────────

    public List<MachineResponse> findAll() {
        return machineRepository.findAllByOrderByMachineIdAsc().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public MachineResponse findById(String machineId) {
        return toResponse(getMachine(machineId));
    }

    // ── Dispatch (Wafer FSM step 1) ───────────────────────────────────────

    @Transactional
    public MachineResponse dispatch(String machineId, DispatchRequest req) {
        Machine machine = getMachine(machineId);

        if (machine.getState() != MachineState.IDLE) {
            throw new InvalidStateTransitionException(machineId, machine.getState().name(), "PROCESSING");
        }

        // Deduplicate by waferCode: if the same wafer code has multiple QUEUE/PENDING_SORTING
        // records (e.g. submitted twice), keep only one to prevent duplicate PROCESSING entries.
        Map<String, WipTask> existingByCode = wipTaskRepository
                .findByStatusIn(List.of(WipStatus.QUEUE, WipStatus.PENDING_SORTING)).stream()
                .filter(t -> req.getWaferCodes().contains(t.getWaferCode())
                          && machine.getExpKey().equals(t.getExpKey()))
                .collect(Collectors.toMap(WipTask::getWaferCode, t -> t, (t1, t2) -> t1));
        List<WipTask> existing = new ArrayList<>(existingByCode.values());

        int currentLoad = wipTaskRepository.findByMachineIdAndStatus(machineId, WipStatus.PROCESSING).size();
        if (currentLoad + req.getWaferCodes().size() > machine.getCapacity()) {
            throw new MachineActionException("Dispatch would exceed machine capacity (" + machine.getCapacity() + ")");
        }

        existing.forEach(t -> {
            t.setMachineId(machineId);
            t.setRecipeId(req.getRecipeId());
            t.setExpKey(req.getExpKey());
            t.setStatus(WipStatus.PROCESSING);
        });
        wipTaskRepository.saveAll(existing);

        List<String> existingCodes = existing.stream().map(WipTask::getWaferCode).toList();
        req.getWaferCodes().stream()
                .filter(code -> !existingCodes.contains(code))
                .forEach(code -> {
                    WipTask t = new WipTask();
                    t.setRequestId(req.getRequestId());
                    t.setWaferCode(code);
                    t.setExpKey(req.getExpKey());
                    t.setMachineId(machineId);
                    t.setRecipeId(req.getRecipeId());
                    t.setStatus(WipStatus.PROCESSING);
                    wipTaskRepository.save(t);
                });

        int totalProcessing = wipTaskRepository.findByMachineIdAndStatus(machineId, WipStatus.PROCESSING).size();
        machine.setState(MachineState.PROCESSING);
        machine.setCurrentUtilization((int) Math.round((double) totalProcessing / machine.getCapacity() * 100));
        Machine saved = machineRepository.save(machine);

        machineLogService.write(machineId, "OPS",
                "Dispatched " + req.getWaferCodes().size() + " wafer(s) [" + String.join(", ", req.getWaferCodes()) + "]. Recipe: " + req.getRecipeId(),
                machine.getCurrentUtilization(), machine.getState().name());
        notificationService.notifyByRoles(WAFER_ROLES, "success",
                "Dispatch Success", req.getWaferCodes().size() + " wafer(s) dispatched to " + machineId);

        // Notify the specific FAB User(s) who submitted these wafers
        java.util.Set<String> dispatchReqIds = new java.util.HashSet<>();
        existing.forEach(t -> { if (t.getRequestId() != null) dispatchReqIds.add(t.getRequestId()); });
        if (req.getRequestId() != null) dispatchReqIds.add(req.getRequestId());
        dispatchReqIds.forEach(reqId -> fabRequestRepository.findById(reqId).ifPresent(fabReq -> {
            if (fabReq.getRequester() != null)
                notificationService.createNotification(fabReq.getRequester().getEmployeeId(),
                    "Wafers In Processing",
                    req.getWaferCodes().size() + " wafer(s) are now being processed on " + machineId + " with recipe " + req.getRecipeId() + ".",
                    "info");
        }));
        return toResponse(saved);
    }

    // ── Unload (Wafer FSM flow 1) ─────────────────────────────────────────

    @Transactional
    public MachineResponse unload(String machineId) {
        Machine machine = getMachine(machineId);

        if (machine.getState() != MachineState.PROCESSING) {
            throw new InvalidStateTransitionException(machineId, machine.getState().name(), "IDLE");
        }

        List<WipTask> tasks = wipTaskRepository.findByMachineIdAndStatus(machineId, WipStatus.PROCESSING);
        tasks.forEach(t -> {
            t.setStatus(WipStatus.COMPLETED);
            t.setCompletedAt(LocalDateTime.now());
        });
        wipTaskRepository.saveAll(tasks);

        machine.setState(MachineState.IDLE);
        machine.setCurrentUtilization(0);
        Machine saved = machineRepository.save(machine);

        List<String> codes = tasks.stream().map(WipTask::getWaferCode).toList();
        machineLogService.write(machineId, "OPS",
                "Unloaded " + codes.size() + " wafer(s) [" + String.join(", ", codes) + "]. Status: COMPLETED",
                machine.getCurrentUtilization(), machine.getState().name());
        notificationService.notifyByRoles(WAFER_ROLES, "success",
                "Unload Success", tasks.size() + " wafer(s) completed on " + machineId);
        notifyRequesters(tasks, "Wafers Completed",
                tasks.size() + " wafer(s) have been successfully unloaded from " + machineId + ". Experiment results are ready.",
                "success");
        return toResponse(saved);
    }

    // ── EMG Unload (Wafer FSM flow 2 & 3) ────────────────────────────────

    @Transactional
    public MachineResponse emgUnload(String machineId, com.tsmc.lims.backend.lab.dto.EmgUnloadRequest.Action action) {
        Machine machine = getMachine(machineId);

        if (machine.getState() != MachineState.PROCESSING) {
            throw new InvalidStateTransitionException(machineId, machine.getState().name(), "IDLE via EMG");
        }

        List<WipTask> tasks = wipTaskRepository.findByMachineIdAndStatus(machineId, WipStatus.PROCESSING);

        List<String> emgCodes = tasks.stream().map(WipTask::getWaferCode).toList();
        String emgCodeStr = "[" + String.join(", ", emgCodes) + "]";
        if (action == com.tsmc.lims.backend.lab.dto.EmgUnloadRequest.Action.REUSE) {
            tasks.forEach(t -> {
                t.setStatus(WipStatus.PENDING_SORTING);
                t.setMachineId(null);
            });
            machine.setState(MachineState.IDLE);
            machine.setCurrentUtilization(0);
            machineLogService.write(machineId, "OPS",
                    "EMG Unload (REUSE): " + emgCodes.size() + " wafer(s) returned to WIP " + emgCodeStr,
                    machine.getCurrentUtilization(), machine.getState().name());
            notificationService.notifyByRoles(WAFER_ROLES, "info",
                    "Wafers Reused", emgCodes.size() + " wafer(s) returned to WIP from " + machineId);
            notifyRequesters(tasks, "Wafers Returned to Queue",
                    emgCodes.size() + " wafer(s) " + emgCodeStr + " were emergency unloaded from " + machineId
                    + " and returned to WIP queue. Re-dispatch when ready.",
                    "warning");
        } else {
            tasks.forEach(t -> {
                t.setStatus(WipStatus.SCRAPPED);
                t.setCompletedAt(LocalDateTime.now());
            });
            machine.setState(MachineState.IDLE);
            machine.setCurrentUtilization(0);
            machineLogService.write(machineId, "OPS",
                    "EMG Unload (SCRAP): " + emgCodes.size() + " wafer(s) scrapped " + emgCodeStr,
                    machine.getCurrentUtilization(), machine.getState().name());
            notificationService.notifyByRoles(WAFER_ROLES, "warning",
                    "Wafers Scrapped", emgCodes.size() + " wafer(s) scrapped from " + machineId);
            notifyRequesters(tasks, "Wafers Scrapped",
                    emgCodes.size() + " wafer(s) " + emgCodeStr + " were scrapped from " + machineId
                    + ". Please submit a new experiment request if required.",
                    "error");
        }
        wipTaskRepository.saveAll(tasks);

        machine.setState(MachineState.IDLE);
        machine.setCurrentUtilization(0);
        machine.setErrorCode(null);
        return toResponse(machineRepository.save(machine));
    }

    // ── Simulate Error (Wafer FSM flow 4 & 5, step A) ────────────────────

    @Transactional
    public MachineResponse simulateError(String machineId) {
        Machine machine = getMachine(machineId);

        if (machine.getState() != MachineState.PROCESSING) {
            throw new InvalidStateTransitionException(machineId, machine.getState().name(), "ALARM");
        }

        machine.setState(MachineState.ALARM);
        machine.setErrorCode("ERR_SIMULATED_FAULT");
        machine.setCurrentUtilization(0);
        Machine saved = machineRepository.save(machine);

        machineLogService.write(machineId, "ALARM", "Simulated fault triggered: ERR_SIMULATED_FAULT",
                machine.getCurrentUtilization(), machine.getState().name());
        notificationService.notifyByRoles(MACHINE_ROLES, "error",
                "System Alert", machineId + " reported a simulated fault.");
        return toResponse(saved);
    }

    // ── Resolve Alarm (Wafer FSM flow 4, step B) ─────────────────────────

    @Transactional
    public MachineResponse resolveAlarm(String machineId) {
        Machine machine = getMachine(machineId);

        if (machine.getState() != MachineState.ALARM) {
            throw new InvalidStateTransitionException(machineId, machine.getState().name(), "PROCESSING");
        }

        int loadedCount = wipTaskRepository.findByMachineIdAndStatus(machineId, WipStatus.PROCESSING).size();
        machine.setState(loadedCount > 0 ? MachineState.PROCESSING : MachineState.IDLE);
        machine.setErrorCode(null);
        machine.setCurrentUtilization(loadedCount > 0
                ? (int) Math.round((double) loadedCount / machine.getCapacity() * 100) : 0);
        Machine saved = machineRepository.save(machine);

        machineLogService.write(machineId, "SYS", "Alarm resolved. Machine back online",
                machine.getCurrentUtilization(), machine.getState().name());
        notificationService.notifyByRoles(MACHINE_ROLES, "success",
                "Alarm Resolved", machineId + " is back online.");
        return toResponse(saved);
    }

    // ── To Maintenance ────────────────────────────────────────────────────
    // Allowed from IDLE (scheduled maintenance) or ALARM (fault-driven maintenance)

    @Transactional
    public MachineResponse toMaintenance(String machineId) {
        Machine machine = getMachine(machineId);

        if (machine.getState() != MachineState.IDLE && machine.getState() != MachineState.ALARM) {
            throw new InvalidStateTransitionException(machineId, machine.getState().name(), "MAINTENANCE");
        }

        machine.setState(MachineState.MAINTENANCE);
        machine.setCurrentUtilization(0);
        Machine saved = machineRepository.save(machine);

        machineLogService.write(machineId, "MAINT", "Machine taken offline for maintenance",
                machine.getCurrentUtilization(), machine.getState().name());
        notificationService.notifyByRoles(MACHINE_ROLES, "info",
                "Machine Offline", machineId + " is now under Maintenance.");
        return toResponse(saved);
    }

    // ── Set Online (Wafer FSM flow 5, step C) ────────────────────────────

    @Transactional
    public MachineResponse setOnline(String machineId) {
        Machine machine = getMachine(machineId);

        if (machine.getState() != MachineState.MAINTENANCE) {
            throw new InvalidStateTransitionException(machineId, machine.getState().name(), "PROCESSING");
        }

        int loadedCount = wipTaskRepository.findByMachineIdAndStatus(machineId, WipStatus.PROCESSING).size();
        machine.setState(loadedCount > 0 ? MachineState.PROCESSING : MachineState.IDLE);
        machine.setErrorCode(null);
        machine.setCurrentUtilization(loadedCount > 0
                ? (int) Math.round((double) loadedCount / machine.getCapacity() * 100) : 0);
        Machine saved = machineRepository.save(machine);

        machineLogService.write(machineId, "MAINT", "Maintenance complete. Machine set online",
                machine.getCurrentUtilization(), machine.getState().name());
        notificationService.notifyByRoles(MACHINE_ROLES, "success",
                "Machine Online", machineId + " resumed processing.");
        return toResponse(saved);
    }

    // ── Experiment Failed Notification ───────────────────────────────────
    // Called after the frontend determines (90% success rate) that processing failed.
    // Notifies the original FAB requester(s) to resubmit their request.

    @Transactional
    public void notifyExperimentFailed(String machineId, List<String> waferCodes) {
        List<WipTask> completedTasks = wipTaskRepository
                .findByMachineIdAndStatus(machineId, WipStatus.COMPLETED)
                .stream()
                .filter(t -> waferCodes.contains(t.getWaferCode()))
                .toList();

        if (!completedTasks.isEmpty()) {
            String codeList = String.join(", ", waferCodes);
            notifyRequesters(completedTasks,
                    "Experiment Failed — Resubmission Required",
                    waferCodes.size() + " wafer(s) [" + codeList + "] failed processing on " + machineId
                            + ". Please resubmit your request.",
                    "error");
        }
    }

    // ── Recipe Management ─────────────────────────────────────────────────

    public List<String> getRecipes(String machineId) {
        if (!machineRepository.existsById(machineId)) {
            throw new ResourceNotFoundException("Machine", machineId);
        }
        return recipeRepository.findByMachineMachineId(machineId).stream()
                .map(Recipe::getName)
                .collect(Collectors.toList());
    }

    @Transactional
    public List<String> addRecipe(String machineId, NameRequest request) {
        Machine machine = getMachine(machineId);

        if (request.getName() == null || request.getName().trim().isEmpty()) {
            throw new MachineActionException("Recipe name cannot be empty.");
        }

        String recipeName = request.getName().trim();
        recipeRepository.save(new Recipe(recipeName, recipeName, machine));

        machineLogService.write(machineId, "CFG", "Recipe added: " + recipeName);
        notificationService.notifyByRoles(MACHINE_ROLES, "success",
                "Recipe Added", "Recipe '" + recipeName + "' added to " + machineId);
        return getRecipes(machineId);
    }

    @Transactional
    public List<String> deleteRecipe(String machineId, String recipeName) {
        if (!machineRepository.existsById(machineId)) {
            throw new ResourceNotFoundException("Machine", machineId);
        }

        Recipe recipe = recipeRepository.findByMachineMachineIdAndName(machineId, recipeName)
                .orElseThrow(() -> new ResourceNotFoundException("Recipe", recipeName));

        recipeRepository.delete(recipe);

        machineLogService.write(machineId, "CFG", "Recipe removed: " + recipeName);
        return getRecipes(machineId);
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    //private String currentUserId() {
    //    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    //    return (auth != null) ? auth.getName() : null;
    //}

    private Machine getMachine(String machineId) {
        return machineRepository.findById(machineId)
                .orElseThrow(() -> new ResourceNotFoundException("Machine", machineId));
    }

    private MachineResponse toResponse(Machine m) {
        List<WipTask> processing = wipTaskRepository.findByMachineIdAndStatus(m.getMachineId(), WipStatus.PROCESSING);
        List<String> loadedWafers = processing.stream().map(WipTask::getWaferCode).collect(Collectors.toList());
        return new MachineResponse(
                m.getMachineId(),
                m.getName(),
                m.getExpKey(),
                m.getState(),
                m.getCapacity(),
                processing.size(),
                loadedWafers,
                m.getErrorCode(),
                m.getCurrentUtilization(),
                m.getOwners()
        );
    }
}

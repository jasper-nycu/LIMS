package com.tsmc.lims.backend.service;

import com.tsmc.lims.backend.dto.DispatchRequest;
import com.tsmc.lims.backend.entity.Machine;
import com.tsmc.lims.backend.entity.WipTask;
import com.tsmc.lims.backend.entity.enums.MachineState;
import com.tsmc.lims.backend.entity.enums.NotificationType;
import com.tsmc.lims.backend.entity.enums.WipStatus;
import com.tsmc.lims.backend.exception.InvalidStateTransitionException;
import com.tsmc.lims.backend.exception.ResourceNotFoundException;
import com.tsmc.lims.backend.repository.MachineRepository;
import com.tsmc.lims.backend.repository.WipTaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DispatchService {

    private final MachineRepository machineRepository;
    private final WipTaskRepository wipTaskRepository;
    private final NotificationService notificationService;

    /**
     * API 1: Dispatch wafers to a machine.
     * Wafer status → PROCESSING, Machine state → PROCESSING.
     */
    @Transactional
    public Machine dispatch(DispatchRequest req) {
        Machine machine = machineRepository.findById(req.getMachineId())
                .orElseThrow(() -> new ResourceNotFoundException("Machine", req.getMachineId()));

        if (machine.getState() != MachineState.IDLE) {
            throw new InvalidStateTransitionException(
                    machine.getMachineId(), machine.getState().name(), MachineState.PROCESSING.name());
        }

        List<WipTask> tasks = wipTaskRepository.findByStatus(WipStatus.QUEUE).stream()
                .filter(t -> req.getWaferCodes().contains(t.getWaferCode()))
                .toList();

        int currentLoad = (int) wipTaskRepository
                .findByMachineIdAndStatus(machine.getMachineId(), WipStatus.PROCESSING).size();

        if (currentLoad + tasks.size() > machine.getCapacity()) {
            throw new IllegalArgumentException(
                    "Dispatch would exceed machine capacity (" + machine.getCapacity() + ")");
        }

        tasks.forEach(t -> {
            t.setMachineId(machine.getMachineId());
            t.setRecipeId(req.getRecipeId());
            t.setStatus(WipStatus.PROCESSING);
        });
        wipTaskRepository.saveAll(tasks);

        // Also create new tasks for wafer codes that don't yet exist in wip_tasks
        List<String> existingCodes = tasks.stream().map(WipTask::getWaferCode).toList();
        req.getWaferCodes().stream()
                .filter(code -> !existingCodes.contains(code))
                .forEach(code -> {
                    WipTask newTask = new WipTask();
                    newTask.setRequestId(req.getRequestId());
                    newTask.setWaferCode(code);
                    newTask.setExpKey(req.getExpKey());
                    newTask.setMachineId(machine.getMachineId());
                    newTask.setRecipeId(req.getRecipeId());
                    newTask.setStatus(WipStatus.PROCESSING);
                    wipTaskRepository.save(newTask);
                });

        int totalProcessing = wipTaskRepository
                .findByMachineIdAndStatus(machine.getMachineId(), WipStatus.PROCESSING).size();
        machine.setState(MachineState.PROCESSING);
        machine.setCurrentUtilization((int) Math.round((double) totalProcessing / machine.getCapacity() * 100));
        Machine saved = machineRepository.save(machine);

        notificationService.emit(
                machine.getMachineId(),
                NotificationType.INFO,
                "Dispatch Initiated",
                req.getWaferCodes().size() + " wafer(s) dispatched to " + machine.getMachineId()
        );

        return saved;
    }
}

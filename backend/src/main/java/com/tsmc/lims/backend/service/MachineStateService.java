package com.tsmc.lims.backend.service;

import com.tsmc.lims.backend.dto.EmgUnloadRequest;
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

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MachineStateService {

    private final MachineRepository machineRepository;
    private final WipTaskRepository wipTaskRepository;
    private final NotificationService notificationService;

    public List<Machine> findAll() {
        return machineRepository.findAll();
    }

    public Machine findById(String machineId) {
        return machineRepository.findById(machineId)
                .orElseThrow(() -> new ResourceNotFoundException("Machine", machineId));
    }

    /**
     * Flow 1: Normal Unload — machine → IDLE, wafers → COMPLETED.
     */
    @Transactional
    public Machine unload(String machineId) {
        Machine machine = findById(machineId);

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

        notificationService.emit(machineId, NotificationType.SUCCESS,
                "Unload Success", tasks.size() + " wafer(s) completed on " + machineId);
        return saved;
    }

    /**
     * Flow 2 & 3: Emergency Unload.
     * REUSE → wafers reset to PENDING_SORTING (back in WIP pool), machine → IDLE.
     * SCRAP → wafers set to SCRAPPED, machine → IDLE.
     */
    @Transactional
    public Machine emgUnload(String machineId, EmgUnloadRequest.Action action) {
        Machine machine = findById(machineId);

        if (machine.getState() != MachineState.PROCESSING) {
            throw new InvalidStateTransitionException(machineId, machine.getState().name(), "IDLE via EMG");
        }

        List<WipTask> tasks = wipTaskRepository.findByMachineIdAndStatus(machineId, WipStatus.PROCESSING);

        if (action == EmgUnloadRequest.Action.REUSE) {
            tasks.forEach(t -> {
                t.setStatus(WipStatus.PENDING_SORTING);
                t.setMachineId(null);
            });
            notificationService.emit(machineId, NotificationType.WARNING,
                    "EMG Unload — Reuse", tasks.size() + " wafer(s) returned to WIP from " + machineId);
        } else {
            tasks.forEach(t -> {
                t.setStatus(WipStatus.SCRAPPED);
                t.setCompletedAt(LocalDateTime.now());
            });
            notificationService.emit(machineId, NotificationType.WARNING,
                    "EMG Unload — Scrap", tasks.size() + " wafer(s) scrapped from " + machineId);
        }
        wipTaskRepository.saveAll(tasks);

        machine.setState(MachineState.IDLE);
        machine.setCurrentUtilization(0);
        machine.setErrorCode(null);
        return machineRepository.save(machine);
    }

    /**
     * Flow 4 (Action A) & 5 (Action A): Simulate Error — machine → ALARM.
     */
    @Transactional
    public Machine simulateError(String machineId) {
        Machine machine = findById(machineId);

        if (machine.getState() != MachineState.PROCESSING) {
            throw new InvalidStateTransitionException(machineId, machine.getState().name(), "ALARM");
        }

        machine.setState(MachineState.ALARM);
        machine.setErrorCode("ERR_SIMULATED_FAULT");
        machine.setCurrentUtilization(0);
        Machine saved = machineRepository.save(machine);

        notificationService.emit(machineId, NotificationType.ERROR,
                "🚨 System Alert", machineId + " reported a simulated fault.");
        return saved;
    }

    /**
     * Flow 4 (Action B): Resolve Alarm — machine → PROCESSING (wafers still loaded).
     */
    @Transactional
    public Machine resolveAlarm(String machineId) {
        Machine machine = findById(machineId);

        if (machine.getState() != MachineState.ALARM) {
            throw new InvalidStateTransitionException(machineId, machine.getState().name(), "PROCESSING");
        }

        int loadedCount = wipTaskRepository.findByMachineIdAndStatus(machineId, WipStatus.PROCESSING).size();

        machine.setState(loadedCount > 0 ? MachineState.PROCESSING : MachineState.IDLE);
        machine.setErrorCode(null);
        machine.setCurrentUtilization(loadedCount > 0
                ? (int) Math.round((double) loadedCount / machine.getCapacity() * 100) : 0);
        Machine saved = machineRepository.save(machine);

        notificationService.emit(machineId, NotificationType.SUCCESS,
                "Alarm Resolved", machineId + " is back online.");
        return saved;
    }

    /**
     * Flow 5 (Action B): ALARM → MAINTENANCE.
     */
    @Transactional
    public Machine toMaintenance(String machineId) {
        Machine machine = findById(machineId);

        if (machine.getState() != MachineState.ALARM) {
            throw new InvalidStateTransitionException(machineId, machine.getState().name(), "MAINTENANCE");
        }

        machine.setState(MachineState.MAINTENANCE);
        machine.setCurrentUtilization(0);
        Machine saved = machineRepository.save(machine);

        notificationService.emit(machineId, NotificationType.WARNING,
                "Machine Offline", machineId + " is now under Maintenance.");
        return saved;
    }

    /**
     * Flow 5 (Action C): MAINTENANCE → PROCESSING (wafers still loaded).
     */
    @Transactional
    public Machine setOnline(String machineId) {
        Machine machine = findById(machineId);

        if (machine.getState() != MachineState.MAINTENANCE) {
            throw new InvalidStateTransitionException(machineId, machine.getState().name(), "PROCESSING");
        }

        int loadedCount = wipTaskRepository.findByMachineIdAndStatus(machineId, WipStatus.PROCESSING).size();

        machine.setState(loadedCount > 0 ? MachineState.PROCESSING : MachineState.IDLE);
        machine.setErrorCode(null);
        machine.setCurrentUtilization(loadedCount > 0
                ? (int) Math.round((double) loadedCount / machine.getCapacity() * 100) : 0);
        Machine saved = machineRepository.save(machine);

        notificationService.emit(machineId, NotificationType.SUCCESS,
                "Machine Online", machineId + " resumed processing.");
        return saved;
    }
}

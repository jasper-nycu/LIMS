package com.tsmc.lims.backend.lab.service;

import com.tsmc.lims.backend.lab.dto.MachineLogEntry;
import com.tsmc.lims.backend.lab.dto.UtilizationPoint;
import com.tsmc.lims.backend.lab.entity.MachineLog;
import com.tsmc.lims.backend.lab.exception.ResourceNotFoundException;
import com.tsmc.lims.backend.lab.repository.MachineLogRepository;
import com.tsmc.lims.backend.lab.repository.MachineRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MachineLogService {

    private final MachineLogRepository logRepository;
    private final MachineRepository machineRepository;

    public void write(String machineId, String level, String message) {
        logRepository.save(new MachineLog(machineId, level, message));
    }

    public void write(String machineId, String level, String message, Integer utilization, String machineState) {
        logRepository.save(new MachineLog(machineId, level, message, utilization, machineState));
    }

    public List<UtilizationPoint> getUtilizationHistory(String machineId, int hoursBack) {
        if (!machineRepository.existsById(machineId)) {
            throw new ResourceNotFoundException("Machine", machineId);
        }
        LocalDateTime since = LocalDateTime.now().minusHours(Math.max(hoursBack, 1));
        return logRepository
                .findByMachineIdAndCreatedAtAfterAndUtilizationIsNotNullOrderByCreatedAtAsc(machineId, since)
                .stream()
                .map(l -> new UtilizationPoint(
                        l.getCreatedAt().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
                        l.getUtilization(),
                        l.getMachineState()))
                .collect(Collectors.toList());
    }

    public List<MachineLogEntry> getLogs(String machineId) {
        if (!machineRepository.existsById(machineId)) {
            throw new ResourceNotFoundException("Machine", machineId);
        }
        return logRepository.findByMachineIdOrderByCreatedAtAsc(machineId)
                .stream()
                .map(l -> new MachineLogEntry(l.getCreatedAt(), l.getLevel(), l.getMessage()))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public String getLogsAsText(String machineId) {
        if (!machineRepository.existsById(machineId)) {
            throw new ResourceNotFoundException("Machine", machineId);
        }
        return logRepository.findByMachineIdOrderByCreatedAtAsc(machineId)
                .stream()
                .map(l -> new MachineLogEntry(l.getCreatedAt(), l.getLevel(), l.getMessage()).toLine())
                .collect(Collectors.joining("\n"));
    }
}

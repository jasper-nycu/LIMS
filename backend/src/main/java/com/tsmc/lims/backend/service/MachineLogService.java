package com.tsmc.lims.backend.service;

import com.tsmc.lims.backend.dto.MachineLogEntry;
import com.tsmc.lims.backend.entity.MachineLog;
import com.tsmc.lims.backend.exception.ResourceNotFoundException;
import com.tsmc.lims.backend.repository.MachineLogRepository;
import com.tsmc.lims.backend.repository.MachineRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

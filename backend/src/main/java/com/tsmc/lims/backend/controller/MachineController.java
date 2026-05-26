package com.tsmc.lims.backend.controller;

import com.tsmc.lims.backend.dto.ApiResponse;
import com.tsmc.lims.backend.dto.EmgUnloadRequest;
import com.tsmc.lims.backend.entity.Machine;
import com.tsmc.lims.backend.service.MachineStateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/machines")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class MachineController {

    private final MachineStateService machineStateService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Machine>>> getAll() {
        return ResponseEntity.ok(ApiResponse.ok("OK", machineStateService.findAll()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Machine>> getById(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.ok("OK", machineStateService.findById(id)));
    }

    /** Flow 1: Normal unload — PROCESSING → IDLE, wafers → COMPLETED */
    @PostMapping("/{id}/unload")
    public ResponseEntity<ApiResponse<Machine>> unload(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.ok("Unload successful", machineStateService.unload(id)));
    }

    /** Flow 2 & 3: Emergency unload (REUSE or SCRAP) */
    @PostMapping("/{id}/emg-unload")
    public ResponseEntity<ApiResponse<Machine>> emgUnload(
            @PathVariable String id,
            @Valid @RequestBody EmgUnloadRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(
                "EMG unload processed", machineStateService.emgUnload(id, req.getAction())));
    }

    /** Flow 4 & 5 (A): Simulate error — PROCESSING → ALARM */
    @PostMapping("/{id}/simulate-error")
    public ResponseEntity<ApiResponse<Machine>> simulateError(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.ok("Error simulated", machineStateService.simulateError(id)));
    }

    /** Flow 4 (B): Resolve alarm — ALARM → PROCESSING */
    @PostMapping("/{id}/resolve-alarm")
    public ResponseEntity<ApiResponse<Machine>> resolveAlarm(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.ok("Alarm resolved", machineStateService.resolveAlarm(id)));
    }

    /** Flow 5 (B): ALARM → MAINTENANCE */
    @PostMapping("/{id}/maintenance")
    public ResponseEntity<ApiResponse<Machine>> toMaintenance(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.ok("Machine under maintenance", machineStateService.toMaintenance(id)));
    }

    /** Flow 5 (C): MAINTENANCE → PROCESSING */
    @PostMapping("/{id}/online")
    public ResponseEntity<ApiResponse<Machine>> setOnline(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.ok("Machine online", machineStateService.setOnline(id)));
    }
}

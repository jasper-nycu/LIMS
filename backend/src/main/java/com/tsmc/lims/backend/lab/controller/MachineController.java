package com.tsmc.lims.backend.lab.controller;

import com.tsmc.lims.backend.lab.dto.ApiResponse;
import com.tsmc.lims.backend.lab.dto.DispatchRequest;
import com.tsmc.lims.backend.lab.dto.EmgUnloadRequest;
import com.tsmc.lims.backend.lab.dto.MachineLogEntry;
import com.tsmc.lims.backend.lab.dto.MachineResponse;
import com.tsmc.lims.backend.lab.dto.NameRequest;
import com.tsmc.lims.backend.lab.dto.UtilizationPoint;
import com.tsmc.lims.backend.lab.service.MachineLogService;
import com.tsmc.lims.backend.lab.service.MachineService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/machines")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class MachineController {

    private final MachineService machineService;
    private final MachineLogService machineLogService;

    @GetMapping
    public ResponseEntity<List<MachineResponse>> getAll() {
        return ResponseEntity.ok(machineService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<MachineResponse>> getById(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.ok("OK", machineService.findById(id)));
    }

    /** Wafer FSM step 1: Dispatch wafers → PROCESSING */
    @PostMapping("/{id}/dispatch")
    public ResponseEntity<ApiResponse<MachineResponse>> dispatch(
            @PathVariable String id,
            @Valid @RequestBody DispatchRequest req) {
        req.setMachineId(id);
        return ResponseEntity.ok(ApiResponse.ok("Dispatch successful", machineService.dispatch(id, req)));
    }

    /** Wafer FSM flow 1: Normal unload → IDLE, wafers → COMPLETED */
    @PostMapping("/{id}/unload")
    public ResponseEntity<ApiResponse<MachineResponse>> unload(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.ok("Unload successful", machineService.unload(id)));
    }

    /** Wafer FSM flow 2 & 3: Emergency unload (REUSE or SCRAP) */
    @PostMapping("/{id}/emg-unload")
    public ResponseEntity<ApiResponse<MachineResponse>> emgUnload(
            @PathVariable String id,
            @Valid @RequestBody EmgUnloadRequest req) {
        return ResponseEntity.ok(ApiResponse.ok("EMG unload processed", machineService.emgUnload(id, req.getAction())));
    }

    /** Wafer FSM flow 4 & 5 (A): Simulate error → ALARM */
    @PostMapping("/{id}/simulate-error")
    public ResponseEntity<ApiResponse<MachineResponse>> simulateError(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.ok("Error simulated", machineService.simulateError(id)));
    }

    /** Wafer FSM flow 4 (B): Resolve alarm → PROCESSING */
    @PostMapping("/{id}/resolve-alarm")
    public ResponseEntity<ApiResponse<MachineResponse>> resolveAlarm(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.ok("Alarm resolved", machineService.resolveAlarm(id)));
    }

    /** Wafer FSM flow 5 (B): ALARM → MAINTENANCE */
    @PostMapping("/{id}/maintenance")
    public ResponseEntity<ApiResponse<MachineResponse>> toMaintenance(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.ok("Machine under maintenance", machineService.toMaintenance(id)));
    }

    /** Wafer FSM flow 5 (C): MAINTENANCE → PROCESSING */
    @PostMapping("/{id}/online")
    public ResponseEntity<ApiResponse<MachineResponse>> setOnline(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.ok("Machine online", machineService.setOnline(id)));
    }

    /** Recipe management */
    @GetMapping("/{id}/recipes")
    public ResponseEntity<ApiResponse<List<String>>> getRecipes(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.ok("OK", machineService.getRecipes(id)));
    }

    @PostMapping("/{id}/recipes")
    public ResponseEntity<ApiResponse<List<String>>> addRecipe(
            @PathVariable String id,
            @RequestBody NameRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Recipe added", machineService.addRecipe(id, request)));
    }

    @DeleteMapping("/{id}/recipes/{name}")
    public ResponseEntity<ApiResponse<List<String>>> deleteRecipe(
            @PathVariable String id,
            @PathVariable String name) {
        return ResponseEntity.ok(ApiResponse.ok("Recipe deleted", machineService.deleteRecipe(id, name)));
    }

    /** Machine event logs */
    @GetMapping("/{id}/logs")
    public ResponseEntity<ApiResponse<List<MachineLogEntry>>> getLogs(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.ok("OK", machineLogService.getLogs(id)));
    }

    @GetMapping("/{id}/utilization-history")
    public ResponseEntity<ApiResponse<List<UtilizationPoint>>> getUtilizationHistory(
            @PathVariable String id,
            @RequestParam(defaultValue = "168") int hours) {
        return ResponseEntity.ok(ApiResponse.ok("OK", machineLogService.getUtilizationHistory(id, hours)));
    }

    @GetMapping("/{id}/logs/download")
    public ResponseEntity<byte[]> downloadLogs(@PathVariable String id) {
        String content = machineLogService.getLogsAsText(id);
        byte[] bytes = content.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + id + "-logs.txt\"")
                .contentType(MediaType.TEXT_PLAIN)
                .body(bytes);
    }
}

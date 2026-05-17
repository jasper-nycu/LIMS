package com.tsmc.lims.backend.machine;

import com.tsmc.lims.backend.machine.dto.DispatchRequest;
import com.tsmc.lims.backend.machine.dto.MachineResponse;
import com.tsmc.lims.backend.machine.dto.NameRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/machines")
@CrossOrigin(origins = "*")
public class MachineController {

    private final MachineService machineService;

    public MachineController(MachineService machineService) {
        this.machineService = machineService;
    }

    @GetMapping
    public List<MachineResponse> listMachines() {
        return machineService.listMachines();
    }

    @GetMapping("/{machineId}")
    public MachineResponse getMachine(@PathVariable String machineId) {
        return machineService.getMachine(machineId);
    }

    @PostMapping("/{machineId}/dispatch")
    public MachineResponse dispatch(@PathVariable String machineId, @RequestBody DispatchRequest request) {
        return machineService.dispatch(machineId, request);
    }

    @PostMapping("/{machineId}/alarm")
    public MachineResponse toggleAlarm(@PathVariable String machineId) {
        return machineService.toggleAlarm(machineId);
    }

    @PostMapping("/{machineId}/maintenance")
    public MachineResponse toggleMaintenance(@PathVariable String machineId) {
        return machineService.toggleMaintenance(machineId);
    }

    @GetMapping("/{machineId}/recipes")
    public List<String> getRecipes(@PathVariable String machineId) {
        return machineService.getRecipes(machineId);
    }

    @PostMapping("/{machineId}/recipes")
    public List<String> addRecipe(@PathVariable String machineId, @RequestBody NameRequest request) {
        return machineService.addRecipe(machineId, request);
    }

    @ExceptionHandler(MachineActionException.class)
    public ResponseEntity<Map<String, String>> handleMachineActionException(MachineActionException ex) {
        Map<String, String> body = new HashMap<>();
        body.put("error", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }
}

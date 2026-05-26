package com.tsmc.lims.backend.controller;

import com.tsmc.lims.backend.dto.ApiResponse;
import com.tsmc.lims.backend.dto.DispatchRequest;
import com.tsmc.lims.backend.entity.Machine;
import com.tsmc.lims.backend.service.DispatchService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class DispatchController {

    private final DispatchService dispatchService;

    /** API 1: POST /api/dispatch */
    @PostMapping("/dispatch")
    public ResponseEntity<ApiResponse<Machine>> dispatch(@Valid @RequestBody DispatchRequest req) {
        Machine machine = dispatchService.dispatch(req);
        return ResponseEntity.ok(ApiResponse.ok("Dispatch successful", machine));
    }
}

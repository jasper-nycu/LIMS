package com.tsmc.lims.backend.lab.controller;

import com.tsmc.lims.backend.lab.dto.ApiResponse;
import com.tsmc.lims.backend.lab.entity.WipTask;
import com.tsmc.lims.backend.lab.service.WipService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/wip")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class WipController {

    private final WipService wipService;

    /** Returns all QUEUE tasks (wafers waiting to be dispatched) */
    @GetMapping
    public ResponseEntity<ApiResponse<List<WipTask>>> getQueue() {
        return ResponseEntity.ok(ApiResponse.ok("OK", wipService.findQueue()));
    }

    /** Returns wafers returned from EMG unload (PENDING_SORTING) */
    @GetMapping("/pending-sorting")
    public ResponseEntity<ApiResponse<List<WipTask>>> getPendingSorting() {
        return ResponseEntity.ok(ApiResponse.ok("OK", wipService.findPendingSorting()));
    }

    @GetMapping("/all")
    public ResponseEntity<ApiResponse<List<WipTask>>> getAll() {
        return ResponseEntity.ok(ApiResponse.ok("OK", wipService.findAll()));
    }
}

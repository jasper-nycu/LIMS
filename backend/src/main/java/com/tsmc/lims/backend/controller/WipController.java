package com.tsmc.lims.backend.controller;

import com.tsmc.lims.backend.dto.ApiResponse;
import com.tsmc.lims.backend.entity.WipTask;
import com.tsmc.lims.backend.service.WipService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/wip")
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

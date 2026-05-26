package com.tsmc.lims.backend.controller;

import com.tsmc.lims.backend.dto.ApiResponse;
import com.tsmc.lims.backend.entity.Notification;
import com.tsmc.lims.backend.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Notification>>> getAll() {
        return ResponseEntity.ok(ApiResponse.ok("OK", notificationService.findAll()));
    }

    @GetMapping("/machine/{machineId}")
    public ResponseEntity<ApiResponse<List<Notification>>> getByMachine(@PathVariable String machineId) {
        return ResponseEntity.ok(ApiResponse.ok("OK", notificationService.findByMachine(machineId)));
    }
}

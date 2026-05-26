package com.tsmc.lims.backend.dto;

public record LoginRequest(
    String empId,
    String password
) {
    public LoginRequest {
        if (empId != null) {
            empId = empId.trim().toUpperCase();
        }
    }
}

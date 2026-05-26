package com.tsmc.lims.backend.auth.dto;

/**
 * Data transfer object for handling user login credentials.
 */
public record LoginRequest(
    String empId,
    String password
) {
    // Compact constructor for input sanitization
    public LoginRequest {
        if (empId != null) {
            empId = empId.trim().toUpperCase();
        }
    }
}
package com.tsmc.lims.backend.auth.dto;

/**
 * Data transfer object for user registration form capture.
 */
public record RegisterRequest(
    String firstName,
    String lastName,
    String email,
    String password,
    String confirmPassword
) {
    // Compact constructor to enforce early baseline validation guardrails
    public RegisterRequest {
        if (email == null || !email.contains("@")) {
            throw new IllegalArgumentException("Validation Error: Please enter a valid email address.");
        }
        if (password == null || !password.equals(confirmPassword)) {
            throw new IllegalArgumentException("Validation Error: Passwords do not match.");
        }
        if (password.length() < 8) {
            throw new IllegalArgumentException("Validation Error: Password must be at least 8 characters long.");
        }
    }
}
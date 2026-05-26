package com.tsmc.lims.backend.dto;

public record RegisterRequest(
    String firstName,
    String lastName,
    String email,
    String password,
    String confirmPassword
) {
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

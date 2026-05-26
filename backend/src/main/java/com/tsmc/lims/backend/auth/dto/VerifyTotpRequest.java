package com.tsmc.lims.backend.auth.dto;

/**
 * Envelope for validating the 6-digit time-based OTP challenge.
 */
public record VerifyTotpRequest(
    String email,
    String code
) {
    public VerifyTotpRequest {
        if (code == null || code.length() != 6 || !code.matches("\\d{6}")) {
            throw new IllegalArgumentException("Validation Error: Please enter a valid 6-digit TOTP code.");
        }
    }
}
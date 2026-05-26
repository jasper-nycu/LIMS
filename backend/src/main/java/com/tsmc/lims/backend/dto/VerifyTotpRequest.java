package com.tsmc.lims.backend.dto;

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

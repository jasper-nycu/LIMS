package com.tsmc.lims.backend.dto;

public record AuthResponse(
    String token,
    UserProfile user
) {}

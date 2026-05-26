package com.tsmc.lims.backend.auth.dto;

/**
 * Combined response containing the authentication token and the user's identity profile.
 */
public record AuthResponse(
    String token,
    UserProfile user
) {}
package com.tsmc.lims.backend.auth.dto;

/**
 * Standardized user identity payload matching frontend UserProfile interface.
 */
public record UserProfile(
    String empId,
    String name,
    String role
) {}
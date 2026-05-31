package com.tsmc.lims.backend.machine.dto;

// DTO record for machine dashboard analytics payload mapping
public record MachineDashboardDto(
    String id,
    String name,
    Integer cap,
    String state,
    Integer currentUtil,
    Integer loadedCount,
    String error
) {}
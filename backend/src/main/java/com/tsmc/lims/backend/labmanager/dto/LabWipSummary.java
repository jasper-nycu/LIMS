package com.tsmc.lims.backend.labmanager.dto;

public record LabWipSummary(
        String id,
        String waferId,
        String expKey,
        String priority
) {
}

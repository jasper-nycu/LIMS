package com.tsmc.lims.backend.labmanager.dto;

public record LabWipSummary(
        String id,
        String waferCode,
        String expKey,
        String priority
) {
}

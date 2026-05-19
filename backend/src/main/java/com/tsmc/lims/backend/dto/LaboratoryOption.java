package com.tsmc.lims.backend.dto;

import java.util.List;

public record LaboratoryOption(
        String labId,
        String labName,
        List<ExperimentOption> experiments
) {
}

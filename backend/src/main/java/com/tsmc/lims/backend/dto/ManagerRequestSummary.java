package com.tsmc.lims.backend.dto;

import java.util.List;

public record ManagerRequestSummary(
        String id,
        String requester,
        String role,
        List<String> waferIds,
        List<String> experiments,
        String remarks,
        String priority,
        String submitTime
) {
}

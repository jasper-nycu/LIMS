package com.tsmc.lims.backend.dto;

import java.time.LocalDateTime;
import java.util.List;

public record FabRequestSummary(
        String id,
        String requesterId,
        String labId,
        List<String> waferIds,
        List<String> experiments,
        int waferCount,
        String status,
        String priority,
        String remarks,
        String rejectReason,
        LocalDateTime createdAt
) {
}

package com.tsmc.lims.backend.fabuser.dto;

import java.util.List;

public record CreateFabRequest(
        String requesterId,
        String labId,
        List<String> experimentKeys,
        List<String> waferIds,
        String priority,
        String remarks
) {
}

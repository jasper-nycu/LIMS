package com.tsmc.lims.backend.labmanager.dto;

public record DecisionRequest(
        String approverId,
        String rejectReason
) {
}

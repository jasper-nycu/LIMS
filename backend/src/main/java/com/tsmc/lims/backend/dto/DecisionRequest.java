package com.tsmc.lims.backend.dto;

public record DecisionRequest(
        String approverId,
        String rejectReason
) {
}

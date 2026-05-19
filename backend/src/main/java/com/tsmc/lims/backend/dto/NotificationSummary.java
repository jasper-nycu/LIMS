package com.tsmc.lims.backend.dto;

import java.time.LocalDateTime;

public record NotificationSummary(
        String id,
        String title,
        String desc,
        String type,
        boolean read,
        LocalDateTime createdAt
) {
}

package com.tsmc.lims.backend.notification.dto;

public record NotificationDto(
    String id,
    String title,
    String desc,
    String type,
    Boolean isRead
) {}
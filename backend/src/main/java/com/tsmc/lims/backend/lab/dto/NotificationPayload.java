package com.tsmc.lims.backend.lab.dto;

public record NotificationPayload(
    String senderId,
    String title,
    String desc,
    String type,
    String target
) {}

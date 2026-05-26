package com.tsmc.lims.backend.notification;

public record NotificationPayload(
    String senderId,
    String title,
    String desc,
    String type,
    String target
) {}

package com.tsmc.lims.backend.controller;

import com.tsmc.lims.backend.dto.NotificationPayload;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
public class WsNotificationController {

    private final SimpMessagingTemplate messagingTemplate;

    public WsNotificationController(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    // Frontend sends to /app/notify → broadcast to all subscribers of /topic/notifications
    @MessageMapping("/notify")
    public void relay(NotificationPayload payload) {
        messagingTemplate.convertAndSend("/topic/notifications", payload);
    }
}

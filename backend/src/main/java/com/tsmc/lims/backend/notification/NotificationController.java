package com.tsmc.lims.backend.notification;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
public class NotificationController {

    private final SimpMessagingTemplate messagingTemplate;

    public NotificationController(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    // Frontend sends to /app/notify → broadcast to all subscribers of /topic/notifications
    @MessageMapping("/notify")
    public void relay(NotificationPayload payload) {
        messagingTemplate.convertAndSend("/topic/notifications", payload);
    }
}

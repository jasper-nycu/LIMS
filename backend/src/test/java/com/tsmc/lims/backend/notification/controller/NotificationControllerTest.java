package com.tsmc.lims.backend.notification.controller;

import com.tsmc.lims.backend.notification.dto.NotificationRequest;
import com.tsmc.lims.backend.notification.entity.Notification;
import com.tsmc.lims.backend.notification.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class NotificationControllerTest {

    private NotificationService notificationService;
    private NotificationController controller;
    private Authentication auth;

    @BeforeEach
    void setUp() {
        notificationService = mock(NotificationService.class);
        controller = new NotificationController(notificationService);
        auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("TS-0001");
    }

    // ── GET /notifications ────────────────────────────────────────────────────

    @Test
    void getMyNotifications_filtersOutSessionTerminated() {
        Notification visible = new Notification();
        visible.setTitle("New Request");
        Notification hidden = new Notification();
        hidden.setTitle("Session Terminated");
        when(notificationService.getNotificationsByUserId("TS-0001"))
                .thenReturn(List.of(visible, hidden));

        ResponseEntity<List<Notification>> response = controller.getMyNotifications(auth);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).hasSize(1);
        assertThat(response.getBody().get(0).getTitle()).isEqualTo("New Request");
    }

    @Test
    void getMyNotifications_returnsEmptyListWhenAllNotificationsFiltered() {
        Notification hidden = new Notification();
        hidden.setTitle("Session Terminated");
        when(notificationService.getNotificationsByUserId("TS-0001")).thenReturn(List.of(hidden));

        ResponseEntity<List<Notification>> response = controller.getMyNotifications(auth);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isEmpty();
    }

    // ── DELETE /notifications ─────────────────────────────────────────────────

    @Test
    void clearMyNotifications_returns204AndDelegatesToService() {
        ResponseEntity<Void> response = controller.clearMyNotifications(auth);

        assertThat(response.getStatusCode().value()).isEqualTo(204);
        verify(notificationService).clearAllNotificationsByUserId("TS-0001");
    }

    // ── DELETE /notifications/{notifId} ──────────────────────────────────────

    @Test
    void deleteOneNotification_returns204AndDelegatesToService() {
        ResponseEntity<Void> response = controller.deleteOneNotification(auth, 5);

        assertThat(response.getStatusCode().value()).isEqualTo(204);
        verify(notificationService).deleteNotificationByIdAndUserId(5, "TS-0001");
    }

    // ── PUT /notifications/read ───────────────────────────────────────────────

    @Test
    void markAllAsRead_returns204AndDelegatesToService() {
        ResponseEntity<Void> response = controller.markAllAsRead(auth);

        assertThat(response.getStatusCode().value()).isEqualTo(204);
        verify(notificationService).markAllAsReadByUserId("TS-0001");
    }

    // ── POST /notifications ───────────────────────────────────────────────────

    @Test
    void createNotification_returns200AndDelegatesToService() {
        ResponseEntity<Void> response = controller.createNotification(
                auth, new NotificationRequest("Alert", "Something happened.", "warning")
        );

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        verify(notificationService).createNotification("TS-0001", "Alert", "Something happened.", "warning");
    }

    // ── POST /notifications/logout-log ────────────────────────────────────────

    @Test
    void logLogoutAction_createsSessionTerminatedNotificationAndReturns200() {
        ResponseEntity<Void> response = controller.logLogoutAction(auth);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        verify(notificationService).createNotification(
                "TS-0001",
                "Session Terminated",
                "You have safely logged out of the system.",
                "info"
        );
    }
}

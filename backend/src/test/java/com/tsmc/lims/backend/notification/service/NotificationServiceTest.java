package com.tsmc.lims.backend.notification.service;

import com.tsmc.lims.backend.auth.entity.User;
import com.tsmc.lims.backend.auth.repository.UserRepository;
import com.tsmc.lims.backend.notification.entity.Notification;
import com.tsmc.lims.backend.notification.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

class NotificationServiceTest {

    private NotificationRepository notificationRepository;
    private UserRepository userRepository;
    private NotificationService service;

    @BeforeEach
    void setUp() {
        notificationRepository = mock(NotificationRepository.class);
        userRepository = mock(UserRepository.class);
        service = new NotificationService(notificationRepository, userRepository);
    }

    // ── createNotification ────────────────────────────────────────────────────

    @Test
    void createNotification_persistsWithCorrectFieldsAndIsReadFalse() {
        service.createNotification("TS-0001", "New Request", "REQ-123 submitted.", "info");

        verify(notificationRepository).save(argThat(n ->
            "TS-0001".equals(n.getUserId()) &&
            "New Request".equals(n.getTitle()) &&
            "REQ-123 submitted.".equals(n.getMessage()) &&
            "info".equals(n.getType()) &&
            Boolean.FALSE.equals(n.getIsRead())
        ));
    }

    @Test
    void createNotification_sessionTerminated_setsIsReadTrue() {
        service.createNotification("TS-0001", "Session Terminated", "You have safely logged out.", "info");

        verify(notificationRepository).save(argThat(n ->
            Boolean.TRUE.equals(n.getIsRead())
        ));
    }

    @Test
    void createNotification_otherTitles_isReadRemainsDefault() {
        service.createNotification("TS-0001", "Request Approved", "REQ-123 approved.", "success");

        verify(notificationRepository).save(argThat(n ->
            !Boolean.TRUE.equals(n.getIsRead())
        ));
    }

    // ── getNotificationsByUserId ──────────────────────────────────────────────

    @Test
    void getNotificationsByUserId_delegatesToRepositoryAndReturnsAll() {
        Notification n1 = new Notification();
        n1.setUserId("TS-0001");
        n1.setTitle("New Request");
        Notification n2 = new Notification();
        n2.setUserId("TS-0001");
        n2.setTitle("Request Approved");
        when(notificationRepository.findByUserIdOrderByCreatedAtDesc("TS-0001")).thenReturn(List.of(n1, n2));

        List<Notification> result = service.getNotificationsByUserId("TS-0001");

        assertThat(result).hasSize(2);
        assertThat(result).extracting(Notification::getTitle)
                .containsExactly("New Request", "Request Approved");
    }

    // ── notifyByRoles ─────────────────────────────────────────────────────────

    @Test
    void notifyByRoles_fanOutCreatesOneNotificationPerActiveUser() {
        User u1 = new User();
        u1.setEmployeeId("MGR-001");
        User u2 = new User();
        u2.setEmployeeId("MGR-002");
        when(userRepository.findByRoleRoleEnumInAndIsActiveTrue(any())).thenReturn(List.of(u1, u2));

        service.notifyByRoles(List.of("ROLE_LAB_MANAGER"), "info", "Dispatch Alert", "Machine loaded.");

        verify(notificationRepository, times(2)).save(any(Notification.class));
    }

    @Test
    void notifyByRoles_noMatchingUsers_savesNothing() {
        when(userRepository.findByRoleRoleEnumInAndIsActiveTrue(any())).thenReturn(List.of());

        service.notifyByRoles(List.of("ROLE_LAB_MANAGER"), "info", "Test", "Test message");

        verify(notificationRepository, never()).save(any());
    }

    // ── deleteNotificationByIdAndUserId ───────────────────────────────────────

    @Test
    void deleteNotificationByIdAndUserId_deletesWhenOwnerMatches() {
        Notification notif = new Notification();
        notif.setUserId("TS-0001");
        when(notificationRepository.findById(42)).thenReturn(Optional.of(notif));

        service.deleteNotificationByIdAndUserId(42, "TS-0001");

        verify(notificationRepository).deleteById(42);
    }

    @Test
    void deleteNotificationByIdAndUserId_doesNotDeleteWhenOwnerMismatch() {
        Notification notif = new Notification();
        notif.setUserId("TS-9999");
        when(notificationRepository.findById(42)).thenReturn(Optional.of(notif));

        service.deleteNotificationByIdAndUserId(42, "TS-0001");

        verify(notificationRepository, never()).deleteById(any());
    }

    @Test
    void deleteNotificationByIdAndUserId_doesNothingWhenNotifNotFound() {
        when(notificationRepository.findById(99)).thenReturn(Optional.empty());

        service.deleteNotificationByIdAndUserId(99, "TS-0001");

        verify(notificationRepository, never()).deleteById(any());
    }

    // ── clearAllNotificationsByUserId ─────────────────────────────────────────

    @Test
    void clearAllNotificationsByUserId_delegatesDeleteToRepository() {
        service.clearAllNotificationsByUserId("TS-0001");

        verify(notificationRepository).deleteByUserId("TS-0001");
    }

    // ── markAllAsReadByUserId ─────────────────────────────────────────────────

    @Test
    void markAllAsReadByUserId_delegatesMarkReadToRepository() {
        service.markAllAsReadByUserId("TS-0001");

        verify(notificationRepository).markAllAsReadByUserId("TS-0001");
    }
}

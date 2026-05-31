package com.tsmc.lims.backend.profile.service;

import com.tsmc.lims.backend.profile.controller.ProfileController;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class ProfileServiceTest {

    private ProfileService profileService;
    private ProfileController controller;
    private Authentication auth;

    @BeforeEach
    void setUp() {
        profileService = mock(ProfileService.class);
        controller = new ProfileController();
        ReflectionTestUtils.setField(controller, "profileService", profileService);

        auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("TS-0001");
    }

    // ── Avatar ──────────────────────────────────────────────────────────────

    @Test
    void updateAvatar_validPayload_returns200WithBody() {
        String avatar = "data:image/png;base64,aGVsbG8=";

        // ProfileService.updateAvatar returns true when the row was updated
        when(profileService.updateAvatar("TS-0001", avatar)).thenReturn(true);

        ResponseEntity<Map<String, Object>> response =
                controller.updateAvatar(auth, new ProfileController.AvatarRequest(avatar));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody())
                .containsEntry("employeeId", "TS-0001")
                .containsEntry("avatarBase64", avatar);

        // Verify the service layer was actually called with correct arguments
        verify(profileService).updateAvatar("TS-0001", avatar);
    }

    @Test
    void updateAvatar_employeeNotFound_returns404() {
        String avatar = "data:image/png;base64,aGVsbG8=";

        // ProfileService.updateAvatar returns false when no row matched
        when(profileService.updateAvatar("TS-0001", avatar)).thenReturn(false);

        ResponseEntity<Map<String, Object>> response =
                controller.updateAvatar(auth, new ProfileController.AvatarRequest(avatar));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void updateAvatar_nonImageMimeType_returns400WithoutCallingService() {
        ResponseEntity<Map<String, Object>> response = controller.updateAvatar(
                auth,
                new ProfileController.AvatarRequest("data:text/plain;base64,aGVsbG8=")
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        // Validation must short-circuit before reaching the service layer
        verify(profileService, never()).updateAvatar(anyString(), anyString());
    }

    @Test
    void updateAvatar_nullRequest_returns400WithoutCallingService() {
        ResponseEntity<Map<String, Object>> response = controller.updateAvatar(auth, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        verify(profileService, never()).updateAvatar(anyString(), anyString());
    }

    @Test
    void updateAvatar_serviceThrows_returns500() {
        String avatar = "data:image/jpeg;base64,aGVsbG8=";
        when(profileService.updateAvatar("TS-0001", avatar))
                .thenThrow(new RuntimeException("DB connection lost"));

        ResponseEntity<Map<String, Object>> response =
                controller.updateAvatar(auth, new ProfileController.AvatarRequest(avatar));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    // ── Password verify ─────────────────────────────────────────────────────

    @Test
    void verifyOldPassword_correctPassword_returns200() {
        // verifyCurrentPassword returns void on success, throws on failure
        doNothing().when(profileService).verifyCurrentPassword("TS-0001", "correct");

        ResponseEntity<Map<String, Object>> response = controller.verifyOldPassword(
                auth,
                new ProfileController.PasswordUpdateRequest("correct", "", null)
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void verifyOldPassword_wrongPassword_returns422NotExpireSession() {
        // 422 must be returned so the frontend interceptor does NOT log the user out
        doThrow(new IllegalArgumentException("Current password is incorrect."))
                .when(profileService).verifyCurrentPassword("TS-0001", "wrong");

        ResponseEntity<Map<String, Object>> response = controller.verifyOldPassword(
                auth,
                new ProfileController.PasswordUpdateRequest("wrong", "", null)
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
        assertThat(response.getBody()).containsKey("message");
    }

    // ── Password update ──────────────────────────────────────────────────────

    @Test
    void updatePassword_success_returns200() {
        doNothing().when(profileService).updatePassword("TS-0001", "old", "new", "123456");

        ResponseEntity<Map<String, Object>> response = controller.updatePassword(
                auth,
                new ProfileController.PasswordUpdateRequest("old", "new", "123456")
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void updatePassword_wrongOldPassword_returns422() {
        doThrow(new IllegalArgumentException("Current password is incorrect."))
                .when(profileService).updatePassword("TS-0001", "bad", "new", null);

        ResponseEntity<Map<String, Object>> response = controller.updatePassword(
                auth,
                new ProfileController.PasswordUpdateRequest("bad", "new", null)
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
    }

    @Test
    void updatePassword_invalid2FACode_returns403() {
        doThrow(new SecurityException("Invalid or expired verification code."))
                .when(profileService).updatePassword("TS-0001", "old", "new", "000000");

        ResponseEntity<Map<String, Object>> response = controller.updatePassword(
                auth,
                new ProfileController.PasswordUpdateRequest("old", "new", "000000")
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }
}
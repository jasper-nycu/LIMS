package com.tsmc.lims.backend.api;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProfileControllerTest {

    @Test
    void updateAvatarStoresBase64DataUrlInUsersAvatarUrl() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        ProfileController controller = new ProfileController();
        ReflectionTestUtils.setField(controller, "jdbcTemplate", jdbcTemplate);
        String avatar = "data:image/png;base64,aGVsbG8=";

        when(jdbcTemplate.update(
                "UPDATE users SET avatar_url = ? WHERE employee_id = ?",
                avatar,
                "TS-0001"
        )).thenReturn(1);

        ResponseEntity<Map<String, Object>> response = controller.updateAvatar(
                "TS-0001",
                new ProfileController.AvatarRequest(avatar)
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody())
                .containsEntry("employeeId", "TS-0001")
                .containsEntry("avatarBase64", avatar);
    }

    @Test
    void updateAvatarRejectsNonImageBase64Payloads() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        ProfileController controller = new ProfileController();
        ReflectionTestUtils.setField(controller, "jdbcTemplate", jdbcTemplate);

        ResponseEntity<Map<String, Object>> response = controller.updateAvatar(
                "TS-0001",
                new ProfileController.AvatarRequest("data:text/plain;base64,aGVsbG8=")
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        verify(jdbcTemplate, never()).update(
                "UPDATE users SET avatar_url = ? WHERE employee_id = ?",
                "data:text/plain;base64,aGVsbG8=",
                "TS-0001"
        );
    }
}

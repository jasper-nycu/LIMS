package com.tsmc.lims.backend.profile.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/users")
public class ProfileController {

    private static final String DATA_IMAGE_PREFIX = "data:image/";

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @GetMapping("/{employeeId}/avatar")
    public ResponseEntity<Map<String, Object>> getAvatar(@PathVariable String employeeId) {
        List<String> avatars = jdbcTemplate.query(
                "SELECT avatar_url FROM users WHERE employee_id = ?",
                (rs, rowNum) -> rs.getString("avatar_url"),
                employeeId
        );

        if (avatars.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Map<String, Object> body = new HashMap<>();
        body.put("employeeId", employeeId);
        body.put("avatarBase64", avatars.get(0));
        return ResponseEntity.ok(body);
    }

    @PutMapping("/{employeeId}/avatar")
    public ResponseEntity<Map<String, Object>> updateAvatar(
            @PathVariable String employeeId,
            @RequestBody AvatarRequest request
    ) {
        if (request == null || !isValidImageDataUrl(request.avatarBase64())) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "avatarBase64 must be a valid data:image/*;base64 payload.");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }

        int updated = jdbcTemplate.update(
                "UPDATE users SET avatar_url = ? WHERE employee_id = ?",
                request.avatarBase64(),
                employeeId
        );

        if (updated == 0) {
            return ResponseEntity.notFound().build();
        }

        Map<String, Object> body = new HashMap<>();
        body.put("employeeId", employeeId);
        body.put("avatarBase64", request.avatarBase64());
        return ResponseEntity.ok(body);
    }

    private boolean isValidImageDataUrl(String value) {
        if (value == null || !value.startsWith(DATA_IMAGE_PREFIX)) {
            return false;
        }

        int commaIndex = value.indexOf(',');
        if (commaIndex <= DATA_IMAGE_PREFIX.length() || commaIndex == value.length() - 1) {
            return false;
        }

        String metadata = value.substring(0, commaIndex).toLowerCase();
        if (!metadata.endsWith(";base64")) {
            return false;
        }

        try {
            Base64.getDecoder().decode(value.substring(commaIndex + 1));
            return true;
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    public record AvatarRequest(String avatarBase64) {
    }
}

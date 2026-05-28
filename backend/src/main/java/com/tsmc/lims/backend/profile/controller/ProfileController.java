package com.tsmc.lims.backend.profile.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;

import com.tsmc.lims.backend.profile.service.ProfileService;

import java.util.Base64;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/users")
public class ProfileController {

    private static final String DATA_IMAGE_PREFIX = "data:image/";

    @Autowired
    private ProfileService profileService;

    @GetMapping("/me/profile")
    public ResponseEntity<Map<String, Object>> getProfile(Authentication authentication) {
        try {
            return ResponseEntity.ok(profileService.getProfile(authentication.getName()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/me/profile")
    public ResponseEntity<Map<String, Object>> updateProfile(Authentication authentication, @RequestBody ProfileUpdateRequest req) {
        try {
            profileService.updateProfile(authentication.getName(), req.title(), req.firstName(), req.lastName(),
                req.department(), req.email(), req.telephone(), req.extension(), req.mobilePhone(), req.twoFactorEnabled(), req.emailTotpCode());
            return ResponseEntity.ok(Map.of("message", "Profile updated successfully."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", e.getMessage()));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/me/profile/email/send-code")
    public ResponseEntity<Map<String, Object>> sendEmailChangeCode(Authentication authentication, @RequestBody EmailChangeRequest req) {
        try {
            profileService.sendEmailChangeCode(authentication.getName(), req.newEmail());
            return ResponseEntity.ok(Map.of("message", "Verification code sent to new email."));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/me/logout")
    public ResponseEntity<Void> logout(Authentication authentication) {
        profileService.setUserInactive(authentication.getName());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/me/avatar")
    public ResponseEntity<Map<String, Object>> getAvatar(Authentication authentication) {
        try {
            String avatarBase64 = profileService.getAvatar(authentication.getName());
            if (avatarBase64 == null) return ResponseEntity.notFound().build();
            return ResponseEntity.ok(Map.of("employeeId", authentication.getName(), "avatarBase64", avatarBase64));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/me/avatar")
    public ResponseEntity<Map<String, Object>> updateAvatar(Authentication authentication, @RequestBody AvatarRequest request) {
        if (request == null || !isValidImageDataUrl(request.avatarBase64())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "avatarBase64 must be a valid data:image/*;base64 payload."));
        }
        try {
            boolean updated = profileService.updateAvatar(authentication.getName(), request.avatarBase64());
            if (!updated) return ResponseEntity.notFound().build();
            return ResponseEntity.ok(Map.of("employeeId", authentication.getName(), "avatarBase64", request.avatarBase64()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", e.getMessage()));
        }
    }

    // Verify the current password before advancing to step 2 of the Change Password modal.
    // Returns 422 (not 401) on wrong password to avoid triggering the frontend's global auth-expired interceptor.
    @PostMapping("/me/password/verify")
    public ResponseEntity<Map<String, Object>> verifyOldPassword(
            Authentication authentication,
            @RequestBody PasswordUpdateRequest request
    ) {
        try {
            profileService.verifyCurrentPassword(authentication.getName(), request.oldPassword());
            return ResponseEntity.ok(Map.of("message", "Password verified."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_CONTENT).body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Verification failed: " + e.getMessage()));
        }
    }

    // Generate and dispatch a 6-digit OTP to the user's registered email for 2FA password change
    @PostMapping("/me/password/send-code")
    public ResponseEntity<Map<String, Object>> sendPasswordChangeCode(Authentication authentication) {
        String employeeId = authentication.getName();
        try {
            profileService.sendPasswordChangeCode(employeeId);
            return ResponseEntity.ok(Map.of("message", "Verification code sent."));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to send verification code: " + e.getMessage()));
        }
    }

    // Update the password after verifying old password and (if 2FA enabled) the OTP code
    @PutMapping("/me/password")
    public ResponseEntity<Map<String, Object>> updatePassword(
            Authentication authentication,
            @RequestBody PasswordUpdateRequest request
    ) {
        String employeeId = authentication.getName();
        try {
            profileService.updatePassword(employeeId, request.oldPassword(), request.newPassword(), request.totpCode());
            return ResponseEntity.ok(Map.of("message", "Password updated successfully."));
        } catch (IllegalArgumentException e) {
            // 422 instead of 401 to prevent the frontend interceptor from treating this as a session expiry
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_CONTENT).body(Map.of("message", e.getMessage()));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Security update rejected: " + e.getMessage()));
        }
    }

    private boolean isValidImageDataUrl(String value) {
        if (value == null || !value.startsWith(DATA_IMAGE_PREFIX)) return false;
        int commaIndex = value.indexOf(',');
        if (commaIndex <= DATA_IMAGE_PREFIX.length() || commaIndex == value.length() - 1) return false;
        try {
            Base64.getDecoder().decode(value.substring(commaIndex + 1));
            return true;
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    // DTO records for request payload mapping
    public record AvatarRequest(String avatarBase64) {}
    public record ProfileUpdateRequest(String title, String firstName, String lastName, String department, String email, String telephone, String extension, String mobilePhone, Boolean twoFactorEnabled, String emailTotpCode) {}
    public record PasswordUpdateRequest(String oldPassword, String newPassword, String totpCode) {} 
    public record EmailChangeRequest(String newEmail) {}
}
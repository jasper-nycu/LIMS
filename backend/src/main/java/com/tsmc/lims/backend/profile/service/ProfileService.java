package com.tsmc.lims.backend.profile.service;

import com.tsmc.lims.backend.shared.service.EmailService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ProfileService {

    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    // SecureRandom replaces java.util.Random to ensure cryptographically safe OTP generation
    private final SecureRandom secureRandom = new SecureRandom();

    // In-memory OTP store keyed by employeeId; replace with Redis in production for TTL support
    private final Map<String, String> pwdOtpStorage = new ConcurrentHashMap<>();
    private final Map<String, String> emailOtpStorage = new ConcurrentHashMap<>();
    
    // Inject NotificationService for audit tracking
    private final com.tsmc.lims.backend.notification.service.NotificationService notificationService;

    public ProfileService(JdbcTemplate jdbcTemplate, PasswordEncoder passwordEncoder, EmailService emailService, com.tsmc.lims.backend.notification.service.NotificationService notificationService) {
        this.jdbcTemplate = jdbcTemplate;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.notificationService = notificationService;
    }

    /**
     * Generates a 6-digit OTP, stores it in memory, and dispatches it via the shared EmailService.
     */
    public void sendPasswordChangeCode(String employeeId) {
        String email = jdbcTemplate.queryForObject(
            "SELECT email FROM users WHERE employee_id = ?", String.class, employeeId
        );

        // Use SecureRandom for cryptographically safe OTP ¡X never use java.util.Random for security codes
        String code = String.format("%06d", secureRandom.nextInt(1_000_000));
        pwdOtpStorage.put(employeeId, code);

        emailService.sendTotpEmail(
            email,
            "[Action Required] LIMS Portal - Password Change Verification",
            "Password Change Request",
            "You have requested to change your LIMS account password. Use the verification code below to confirm:",
            code
        );
    }

    /**
     * Generates a 6-digit OTP for email change and dispatches it to the NEW email address.
     */
    public void sendEmailChangeCode(String employeeId, String newEmail) {
        String code = String.format("%06d", secureRandom.nextInt(1_000_000));
        emailOtpStorage.put(employeeId, code);

        emailService.sendTotpEmail(
            newEmail,
            "[Action Required] LIMS Portal - Email Update Verification",
            "Email Address Update",
            "You have requested to update the email address associated with your LIMS account. Use the verification code below to confirm this change:",
            code
        );
    }

    /**
     * Verifies the current password.
     * Throws IllegalArgumentException on mismatch so the controller can return 422,
     * avoiding 401 which would trigger the global auth-expired interceptor on the frontend.
     */
    public void verifyCurrentPassword(String employeeId, String rawPassword) {
        String currentHash = jdbcTemplate.queryForObject(
            "SELECT password_hash FROM users WHERE employee_id = ?", String.class, employeeId
        );
        if (currentHash == null || !passwordEncoder.matches(rawPassword, currentHash)) {
            throw new IllegalArgumentException("Current password is incorrect.");
        }
    }

    /**
     * Fetches the full user profile including password modification timestamp.
     */
    public Map<String, Object> getProfile(String employeeId) {
        return jdbcTemplate.queryForMap(
            "SELECT employee_id, is_active, title, first_name, last_name, department, email, telephone, extension, mobile_phone, two_factor_enabled, password_modified_at FROM users WHERE employee_id = ?",
            employeeId
        );
    }

    /**
     * Updates the user's personal information fields. Validates OTP if email is changed.
     */
    public void updateProfile(String employeeId, String title, String firstName, String lastName,
                               String department, String email, String telephone, String extension,
                               String mobilePhone, Boolean twoFactorEnabled, String emailTotpCode) {
        
        // 1. Strict Backend Mobile Phone Format Validation
        if (mobilePhone != null && !mobilePhone.isEmpty() && !mobilePhone.matches("^09\\d{2}-\\d{3}-\\d{3}$")) {
            throw new IllegalArgumentException("Mobile phone must strictly follow the 09XX-XXX-XXX format.");
        }

        // 2. Check if email has been modified
        String currentEmail = jdbcTemplate.queryForObject(
            "SELECT email FROM users WHERE employee_id = ?", String.class, employeeId
        );
        
        if (currentEmail != null && !currentEmail.equals(email)) {
            String storedCode = emailOtpStorage.get(employeeId);
            if (storedCode == null || emailTotpCode == null || !storedCode.equals(emailTotpCode)) {
                throw new SecurityException("Invalid or expired email verification code.");
            }
            // Invalidate OTP after success
            emailOtpStorage.remove(employeeId);
        }

        jdbcTemplate.update(
            "UPDATE users SET title = ?, first_name = ?, last_name = ?, department = ?, email = ?, telephone = ?, extension = ?, mobile_phone = ?, two_factor_enabled = ? WHERE employee_id = ?",
            title, firstName, lastName, department, email, telephone, extension, mobilePhone, twoFactorEnabled, employeeId
        );

        // Audit Trail: Log profile modification
        notificationService.createNotification(
            employeeId, 
            "Profile Synchronized", 
            "Your personal information and security settings have been updated.", 
            "success"
        );
    }

    /**
     * Marks the user as offline (is_active = false) on logout.
     */
    public void setUserInactive(String employeeId) {
        jdbcTemplate.update("UPDATE users SET is_active = false WHERE employee_id = ?", employeeId);
    }

    /**
     * Returns the user's avatar as a Base64 data URL, or null if not set.
     */
    public String getAvatar(String employeeId) {
        List<String> avatars = jdbcTemplate.query(
            "SELECT avatar_url FROM users WHERE employee_id = ?",
            (rs, rowNum) -> rs.getString("avatar_url"),
            employeeId
        );
        if (avatars.isEmpty() || avatars.get(0) == null) return null;
        return avatars.get(0);
    }

    /**
     * Persists the Base64-encoded avatar image to the database.
     * Returns false if no row was updated (employee not found).
     */
    public boolean updateAvatar(String employeeId, String avatarBase64) {
        int updated = jdbcTemplate.update(
            "UPDATE users SET avatar_url = ? WHERE employee_id = ?",
            avatarBase64, employeeId
        );
        return updated > 0;
    }

    /**
     * Updates the password after verifying old password and (if 2FA enabled) the OTP code.
     */
    public void updatePassword(String employeeId, String oldPassword, String newPassword, String totpCode) {
        Map<String, Object> row = jdbcTemplate.queryForMap(
            "SELECT password_hash, two_factor_enabled FROM users WHERE employee_id = ?", employeeId
        );
        String currentHash = (String) row.get("password_hash");
        Boolean mfaEnabled = (Boolean) row.get("two_factor_enabled");

        if (currentHash == null || !passwordEncoder.matches(oldPassword, currentHash)) {
            throw new IllegalArgumentException("Current password is incorrect.");
        }

        if (Boolean.TRUE.equals(mfaEnabled)) {
            String storedCode = pwdOtpStorage.get(employeeId);
            if (storedCode == null || totpCode == null || !storedCode.equals(totpCode)) {
                throw new SecurityException("Invalid or expired verification code.");
            }
            // Invalidate the OTP immediately after successful use to prevent replay attacks
            pwdOtpStorage.remove(employeeId);
        }

        jdbcTemplate.update(
            "UPDATE users SET password_hash = ?, password_modified_at = NOW() WHERE employee_id = ?",
            passwordEncoder.encode(newPassword), employeeId
        );

        // Audit Trail: Log password change
        notificationService.createNotification(
            employeeId, 
            "Security Alert", 
            "Your account password was successfully changed.", 
            "warning"
        );
    }
}
package com.tsmc.lims.backend.service;

import com.tsmc.lims.backend.dto.AuthResponse;
import com.tsmc.lims.backend.dto.LoginRequest;
import com.tsmc.lims.backend.dto.RegisterRequest;
import com.tsmc.lims.backend.dto.UserProfile;
import com.tsmc.lims.backend.dto.VerifyTotpRequest;
import com.tsmc.lims.backend.entity.Role;
import com.tsmc.lims.backend.entity.User;
import com.tsmc.lims.backend.repository.RoleRepository;
import com.tsmc.lims.backend.repository.UserRepository;
import com.tsmc.lims.backend.security.JwtProvider;
import com.tsmc.lims.backend.security.EcdsaCryptoProvider;
import com.warrenstrange.googleauth.GoogleAuthenticator;
import com.warrenstrange.googleauth.GoogleAuthenticatorKey;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.transaction.annotation.Transactional;
import java.util.regex.Pattern;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final JavaMailSender mailSender;
    private final EcdsaCryptoProvider cryptoProvider;

    private final GoogleAuthenticator gAuth = new GoogleAuthenticator();
    private final Map<String, RegistrationContext> registrationCache = new ConcurrentHashMap<>();

    private record RegistrationContext(RegisterRequest request, String totpSecret) {}

    @PersistenceContext
    private EntityManager entityManager;

    public AuthService(UserRepository userRepository, RoleRepository roleRepository,
                       PasswordEncoder passwordEncoder, JwtProvider jwtProvider,
                       JavaMailSender mailSender, EcdsaCryptoProvider cryptoProvider) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtProvider = jwtProvider;
        this.mailSender = mailSender;
        this.cryptoProvider = cryptoProvider;
    }

    public AuthResponse authenticateUser(LoginRequest request) {
        User user = userRepository.findById(request.empId())
                .orElseThrow(() -> new IllegalArgumentException("Authentication failed: User not found."));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Authentication failed: Incorrect password.");
        }

        String token = jwtProvider.generateToken(user);
        String fullName = user.getFirstName() + " " + user.getLastName();
        UserProfile profile = new UserProfile(fullName, user.getRole().getRoleEnum());

        return new AuthResponse(token, profile);
    }

    public void validateRegistrationInitiation(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("Email already registered in LIMS database.");
        }

        GoogleAuthenticatorKey key = gAuth.createCredentials();
        String secret = key.getKey();
        String totpPassword = String.format("%06d", gAuth.getTotpPassword(secret));

        registrationCache.put(request.email(), new RegistrationContext(request, secret));
        sendTotpEmail(request.email(), totpPassword);
    }

    @Transactional
    public void verifyAndProvisionUser(VerifyTotpRequest request) {
        RegistrationContext context = registrationCache.get(request.email());
        if (context == null) {
            throw new IllegalArgumentException("Verification failure: Session expired or invalid email.");
        }

        boolean isCodeValid = gAuth.authorize(context.totpSecret(), Integer.parseInt(request.code()));
        if (!isCodeValid) {
            throw new IllegalArgumentException("Verification failure: Invalid or expired code.");
        }

        provisionNewUser(context);
        registrationCache.remove(request.email());
    }

    private void provisionNewUser(RegistrationContext context) {
        RegisterRequest req = context.request();

        Role defaultRole = roleRepository.findById("ROLE_PUBLIC")
                .orElseThrow(() -> new IllegalStateException("Database error: Default role not found."));

        Number nextVal = (Number) entityManager.createNativeQuery("SELECT nextval('user_emp_id_seq')").getSingleResult();
        String formattedEmpId = String.format("TS-%04d", nextVal.longValue());

        User newUser = new User();
        newUser.setEmployeeId(formattedEmpId);
        newUser.setFirstName(req.firstName());
        newUser.setLastName(req.lastName());
        newUser.setEmail(req.email());
        newUser.setRole(defaultRole);
        newUser.setPasswordHash(passwordEncoder.encode(req.password()));
        newUser.setPasswordSalt("BCRYPT_EMBEDDED");
        newUser.setTwoFactorEnabled(true);
        newUser.setTotpSecret(context.totpSecret());

        java.security.KeyPair keyPair = cryptoProvider.generateKeyPair();
        newUser.setPublicKey(cryptoProvider.encodeKeyToBase64(keyPair.getPublic()));
        String encryptedPrivKey = cryptoProvider.encryptPrivateKey(keyPair.getPrivate());
        newUser.setEncryptedPrivateKey(encryptedPrivKey);

        userRepository.save(newUser);
    }

    private void sendTotpEmail(String toAddress, String code) {
        String emailRegex = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,6}$";
        Pattern pattern = Pattern.compile(emailRegex);

        if (toAddress == null || !pattern.matcher(toAddress).matches()) {
            throw new IllegalArgumentException("Email dispatch failed: Invalid recipient address format.");
        }

        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setTo(toAddress);
            helper.setSubject("[Action Required] LIMS Portal - Authentication Code");

            String htmlContent = """
                <div style="font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background-color: #f8fafc; padding: 40px 20px; margin: 0;">
                    <div style="max-width: 600px; margin: 0 auto; background-color: #ffffff; border-radius: 8px; overflow: hidden; box-shadow: 0 4px 6px -1px rgba(0,0,0,0.1);">
                        <div style="background-color: #0f172a; padding: 24px; text-align: center;">
                            <h2 style="color: #ffffff; margin: 0; font-size: 24px; letter-spacing: 2px; text-transform: uppercase;">LIMS Portal</h2>
                        </div>
                        <div style="padding: 32px; color: #334155;">
                            <h3 style="margin-top: 0; color: #0f172a; font-size: 20px;">Identity Verification</h3>
                            <p style="font-size: 16px; line-height: 1.6;">Hello,</p>
                            <p style="font-size: 16px; line-height: 1.6;">You are receiving this email because a registration or login attempt was made for the Laboratory Information Management System. Please use the following 6-digit verification code to complete your process:</p>
                            <div style="text-align: center; margin: 36px 0;">
                                <span style="display: inline-block; font-size: 36px; font-weight: bold; color: #2563eb; background-color: #f1f5f9; padding: 16px 32px; border-radius: 8px; letter-spacing: 12px; border: 1px solid #e2e8f0;">%s</span>
                            </div>
                            <p style="font-size: 14px; color: #64748b; line-height: 1.6; padding: 12px; background-color: #fffbeb; border-left: 4px solid #f59e0b; border-radius: 4px;">
                                <strong>Security Alert:</strong> This code will expire shortly. Do not share this code with anyone.
                            </p>
                        </div>
                        <div style="background-color: #f8fafc; border-top: 1px solid #e2e8f0; padding: 24px; text-align: center;">
                            <p style="font-size: 12px; color: #94a3b8; margin: 0;">This is an automated message generated by the TSMC LIMS system.</p>
                        </div>
                    </div>
                </div>
                """.formatted(code);

            helper.setText(htmlContent, true);
            mailSender.send(mimeMessage);

        } catch (MessagingException e) {
            throw new RuntimeException("CRITICAL: Failed to construct and send HTML email.", e);
        }
    }
}

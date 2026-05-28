package com.tsmc.lims.backend.auth.service;

import com.tsmc.lims.backend.auth.dto.*;
import com.tsmc.lims.backend.auth.entity.Role;
import com.tsmc.lims.backend.auth.entity.User;
import com.tsmc.lims.backend.auth.repository.RoleRepository;
import com.tsmc.lims.backend.auth.repository.UserRepository;
import com.tsmc.lims.backend.auth.security.JwtProvider;
import com.tsmc.lims.backend.shared.service.EmailService;
import com.tsmc.lims.backend.auth.security.EcdsaCryptoProvider;
import com.warrenstrange.googleauth.GoogleAuthenticator;
import com.warrenstrange.googleauth.GoogleAuthenticatorKey;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.transaction.annotation.Transactional;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final EcdsaCryptoProvider cryptoProvider;
    private final EmailService emailService;
    private final com.tsmc.lims.backend.notification.service.NotificationService notificationService;
    
    // RFC 6238 TOTP Engine
    private final GoogleAuthenticator gAuth = new GoogleAuthenticator();

    // Simulating Redis for temporary storage of registration sessions (Email -> Context)
    // In a production multi-node cluster, this MUST be replaced with Redis/Memcached.
    private final Map<String, RegistrationContext> registrationCache = new ConcurrentHashMap<>();

    // Internal Java 25 Record to hold pending registration data securely
    private record RegistrationContext(RegisterRequest request, String totpSecret) {}

    @PersistenceContext
    private EntityManager entityManager;

    public AuthService(UserRepository userRepository, RoleRepository roleRepository,
                       PasswordEncoder passwordEncoder, JwtProvider jwtProvider,
                       EcdsaCryptoProvider cryptoProvider, EmailService emailService,
                       com.tsmc.lims.backend.notification.service.NotificationService notificationService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtProvider = jwtProvider;
        this.cryptoProvider = cryptoProvider;
        this.emailService = emailService;
        this.notificationService = notificationService;
    }

    /**
     * CORE: Validates credentials and issues a JWT session token.
     */
    public AuthResponse authenticateUser(LoginRequest request) {
        User user = userRepository.findById(request.empId())
                .orElseThrow(() -> new IllegalArgumentException("Authentication failed: User not found."));

        // Compare raw password from frontend with BCrypt hash in the database
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Authentication failed: Incorrect password.");
        }

        // Set user to active (online) upon successful login
        user.setIsActive(true);
        userRepository.save(user);

        // Generate actual JWT Token
        String token = jwtProvider.generateToken(user);

        // Audit Trail: Log successful authentication initialization
        notificationService.createNotification(
            user.getEmployeeId(),
            "Session Started",
            "You have successfully authenticated and logged into the system.",
            "info"
        );

        String fullName = user.getFirstName() + " " + user.getLastName();
        UserProfile profile = new UserProfile(
            user.getEmployeeId(),
            fullName,
            user.getRole().getRoleEnum()
        );

        return new AuthResponse(token, profile);
    }
    /**
     * CORE: Pre-validates new users and dispatches TOTP via Email.
     */
    public void validateRegistrationInitiation(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("Email already registered in LIMS database.");
        }

        // Generate a new RFC 6238 Secret Key for this session
        GoogleAuthenticatorKey key = gAuth.createCredentials();
        String secret = key.getKey();
        String totpPassword = String.format("%06d", gAuth.getTotpPassword(secret));

        // Cache the request and secret
        registrationCache.put(request.email(), new RegistrationContext(request, secret));

        // Delegate email dispatch to the shared EmailService
        emailService.sendTotpEmail(
            request.email(),
            "[Action Required] LIMS Portal - Authentication Code",
            "Identity Verification",
            "You are receiving this email because a registration attempt was made for the Laboratory Information Management System. Please use the following 6-digit verification code to complete your registration:",
            totpPassword
        );
    }

    /**
     * CORE: Verifies the TOTP code and commits the user to PostgreSQL.
     */
    @Transactional // Ensures Native Sequence and User Save are committed together securely
    public void verifyAndProvisionUser(VerifyTotpRequest request) {
        RegistrationContext context = registrationCache.get(request.email());
        if (context == null) {
            throw new IllegalArgumentException("Verification failure: Session expired or invalid email.");
        }

        // Validate the 6-digit code against the TOTP engine using the cached secret
        boolean isCodeValid = gAuth.authorize(context.totpSecret(), Integer.parseInt(request.code()));
        if (!isCodeValid) {
            throw new IllegalArgumentException("Verification failure: Invalid or expired code.");
        }

        // Proceed to provision the account
        provisionNewUser(context);

        // Clear cache to prevent replay attacks
        registrationCache.remove(request.email());
    }

    /**
     * HELPER: Assembles the JPA Entity and saves to DB.
     * Uses a PostgreSQL sequence to generate a sequential Employee ID (e.g., TS-0001).
     */
    private void provisionNewUser(RegistrationContext context) {
        RegisterRequest req = context.request();
        
        // Fetch default role ('ROLE_PUBLIC' exists in init.sql for strict isolation)
        Role defaultRole = roleRepository.findById("ROLE_PUBLIC")
                .orElseThrow(() -> new IllegalStateException("Database error: Default role not found."));

        // Fetch the next value from PostgreSQL sequence
        Number nextVal = (Number) entityManager.createNativeQuery("SELECT nextval('user_emp_id_seq')").getSingleResult();
        
        // Format the ID to be left-padded with zeros (e.g., TS-0001, TS-0002)
        String formattedEmpId = String.format("TS-%04d", nextVal.longValue());

        User newUser = new User();
        newUser.setEmployeeId(formattedEmpId);
        newUser.setFirstName(req.firstName());
        newUser.setLastName(req.lastName());
        newUser.setEmail(req.email());
        newUser.setRole(defaultRole); // Minimum privilege assigned by default
        
        // Hash the password before saving! (Crucial Security Step)
        newUser.setPasswordHash(passwordEncoder.encode(req.password()));
        newUser.setPasswordSalt("BCRYPT_EMBEDDED"); // BCrypt handles salt internally, string kept for schema compliance
        
        newUser.setTwoFactorEnabled(true);
        newUser.setTotpSecret(context.totpSecret());

        // Generate and store ECDSA cryptographic keys for non-repudiation
        java.security.KeyPair keyPair = cryptoProvider.generateKeyPair();
        newUser.setPublicKey(cryptoProvider.encodeKeyToBase64(keyPair.getPublic()));
        
        // Encrypt the private key using AES-256 before database persistence.
        String encryptedPrivKey = cryptoProvider.encryptPrivateKey(keyPair.getPrivate());
        newUser.setEncryptedPrivateKey(encryptedPrivKey);

        userRepository.save(newUser);
    }
}
package com.tsmc.lims.backend.auth.controller;

import com.tsmc.lims.backend.auth.dto.*;
import com.tsmc.lims.backend.auth.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController // All endpoints in this controller will produce JSON responses by default, aligning with frontend expectations for API communication.
@RequestMapping("/api/v1/auth") // api: separate frontend (HTML) and backend (REST API); v1: versioning for future-proofing and backward compatibility.
@CrossOrigin(origins = "http://localhost:5173") // Align with frontend Vite dev server port
public class AuthController {

    // Controller now ONLY depends on Service, NEVER the Repository.
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
        // Delegate all DB lookups and validation to the Service layer
        AuthResponse response = authService.authenticateUser(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/register/initiate")
    public ResponseEntity<Map<String, String>> initiateRegister(@RequestBody RegisterRequest request) {
        // Delegate duplication checks and email dispatching to Service
        authService.validateRegistrationInitiation(request);
        return ResponseEntity.ok(Map.of("message", "A new verification code has been sent to your email."));
    }

    @PostMapping("/register/verify")
    public ResponseEntity<Map<String, String>> verifyRegister(@RequestBody VerifyTotpRequest request) {
        // Delegate TOTP validation and DB saving to Service
        authService.verifyAndProvisionUser(request);
        return ResponseEntity.ok(Map.of("message", "Registration successful! Please sign in with your new account."));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleValidationExceptions(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
    }
}
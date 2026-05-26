package com.tsmc.lims.backend.controller;

import com.tsmc.lims.backend.dto.AuthResponse;
import com.tsmc.lims.backend.dto.LoginRequest;
import com.tsmc.lims.backend.dto.RegisterRequest;
import com.tsmc.lims.backend.dto.VerifyTotpRequest;
import com.tsmc.lims.backend.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@CrossOrigin(origins = "http://localhost:5173")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
        AuthResponse response = authService.authenticateUser(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/register/initiate")
    public ResponseEntity<Map<String, String>> initiateRegister(@RequestBody RegisterRequest request) {
        authService.validateRegistrationInitiation(request);
        return ResponseEntity.ok(Map.of("message", "A new verification code has been sent to your email."));
    }

    @PostMapping("/register/verify")
    public ResponseEntity<Map<String, String>> verifyRegister(@RequestBody VerifyTotpRequest request) {
        authService.verifyAndProvisionUser(request);
        return ResponseEntity.ok(Map.of("message", "Registration successful! Please sign in with your new account."));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleValidationExceptions(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
    }
}

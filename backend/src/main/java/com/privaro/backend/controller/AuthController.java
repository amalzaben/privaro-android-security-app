package com.privaro.backend.controller;

import com.privaro.backend.dto.*;
import com.privaro.backend.service.AuthService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        logger.info("Registration request for: {}", request.getEmail());
        AuthResponse response = authService.register(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        logger.info("Login request for: {}", request.getEmail());
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/google")
    public ResponseEntity<AuthResponse> googleSignIn(@Valid @RequestBody GoogleAuthRequest request) {
        logger.info("Google sign-in request received");
        AuthResponse response = authService.googleSignIn(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        logger.info("Token refresh request received");
        AuthResponse response = authService.refreshToken(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/password-reset")
    public ResponseEntity<Map<String, String>> sendPasswordResetEmail(@Valid @RequestBody PasswordResetRequest request) {
        logger.info("Password reset request for: {}", request.getEmail());
        authService.sendPasswordResetEmail(request);
        return ResponseEntity.ok(Map.of("message", "Password reset email sent"));
    }

    @GetMapping("/me")
    public ResponseEntity<AuthResponse.UserDto> getCurrentUser(@AuthenticationPrincipal UserDetails userDetails) {
        logger.info("Get current user request for: {}", userDetails.getUsername());
        AuthResponse.UserDto user = authService.getCurrentUser(userDetails.getUsername());
        return ResponseEntity.ok(user);
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(@AuthenticationPrincipal UserDetails userDetails) {
        logger.info("Logout request for: {}", userDetails.getUsername());
        authService.logout(userDetails.getUsername());
        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }
}

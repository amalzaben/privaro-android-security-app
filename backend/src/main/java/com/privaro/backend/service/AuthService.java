package com.privaro.backend.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.privaro.backend.dto.*;
import com.privaro.backend.entity.User;
import com.privaro.backend.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final GoogleTokenVerifier googleTokenVerifier;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            AuthenticationManager authenticationManager,
            GoogleTokenVerifier googleTokenVerifier) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
        this.googleTokenVerifier = googleTokenVerifier;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already registered");
        }

        User user = User.builder()
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .displayName(request.getDisplayName())
                .authProvider(User.AuthProvider.EMAIL)
                .build();

        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        user.setRefreshToken(refreshToken);
        userRepository.save(user);

        logger.info("User registered: {}", user.getEmail());

        return buildAuthResponse(user, accessToken, refreshToken);
    }

    public AuthResponse login(LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );
        } catch (AuthenticationException e) {
            logger.warn("Login failed for {}: {}", request.getEmail(), e.getMessage());
            throw new RuntimeException("Invalid email or password");
        }

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        user.setRefreshToken(refreshToken);
        userRepository.save(user);

        logger.info("User logged in: {}", user.getEmail());

        return buildAuthResponse(user, accessToken, refreshToken);
    }

    @Transactional
    public AuthResponse googleSignIn(GoogleAuthRequest request) {
        GoogleIdToken.Payload payload = googleTokenVerifier.verify(request.getIdToken());
        if (payload == null) {
            throw new RuntimeException("Invalid Google ID token");
        }

        String googleId = payload.getSubject();
        String email = payload.getEmail();
        String name = (String) payload.get("name");

        User user = userRepository.findByGoogleId(googleId)
                .orElseGet(() -> userRepository.findByEmail(email)
                        .map(existingUser -> {
                            // Link Google account to existing email user
                            existingUser.setGoogleId(googleId);
                            if (existingUser.getDisplayName() == null) {
                                existingUser.setDisplayName(name);
                            }
                            return existingUser;
                        })
                        .orElseGet(() -> {
                            // Create new Google user
                            return User.builder()
                                    .email(email)
                                    .displayName(name)
                                    .googleId(googleId)
                                    .authProvider(User.AuthProvider.GOOGLE)
                                    .build();
                        }));

        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        user.setRefreshToken(refreshToken);
        userRepository.save(user);

        logger.info("Google sign-in successful: {}", user.getEmail());

        return buildAuthResponse(user, accessToken, refreshToken);
    }

    public AuthResponse refreshToken(RefreshTokenRequest request) {
        String refreshToken = request.getRefreshToken();

        if (!jwtService.isTokenValid(refreshToken)) {
            throw new RuntimeException("Invalid refresh token");
        }

        String email = jwtService.extractUsername(refreshToken);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!refreshToken.equals(user.getRefreshToken())) {
            throw new RuntimeException("Refresh token mismatch");
        }

        String newAccessToken = jwtService.generateAccessToken(user);
        String newRefreshToken = jwtService.generateRefreshToken(user);

        user.setRefreshToken(newRefreshToken);
        userRepository.save(user);

        logger.info("Token refreshed for: {}", user.getEmail());

        return buildAuthResponse(user, newAccessToken, newRefreshToken);
    }

    public void sendPasswordResetEmail(PasswordResetRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getAuthProvider() == User.AuthProvider.GOOGLE) {
            throw new RuntimeException("This account uses Google Sign-In. Password reset is not available.");
        }

        // TODO: Implement email sending logic
        // For now, just log the request
        logger.info("Password reset requested for: {}", user.getEmail());
    }

    public AuthResponse.UserDto getCurrentUser(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return AuthResponse.UserDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .displayName(user.getDisplayName())
                .authProvider(user.getAuthProvider().name())
                .build();
    }

    @Transactional
    public void logout(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setRefreshToken(null);
        userRepository.save(user);

        logger.info("User logged out: {}", email);
    }

    private AuthResponse buildAuthResponse(User user, String accessToken, String refreshToken) {
        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtService.getAccessTokenExpiration() / 1000) // Convert to seconds
                .user(AuthResponse.UserDto.builder()
                        .id(user.getId())
                        .email(user.getEmail())
                        .displayName(user.getDisplayName())
                        .authProvider(user.getAuthProvider().name())
                        .build())
                .build();
    }
}

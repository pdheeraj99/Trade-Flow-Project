package com.tradeflow.auth.controller;

import com.tradeflow.auth.dto.*;
import com.tradeflow.auth.security.JwtService;
import com.tradeflow.auth.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST Controller for authentication endpoints.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;
    private final JwtService jwtService;

    /**
     * Register a new user
     */
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        log.info("Registration request for: {}", request.getUsername());
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Login and get tokens
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        log.info("Login request for: {}", request.getUsernameOrEmail());
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Refresh access token
     */
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        log.debug("Token refresh request");
        AuthResponse response = authService.refreshToken(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Logout - revoke all refresh tokens
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.substring(7); // Remove "Bearer "
        UUID userId = jwtService.extractUserId(token);
        authService.logout(userId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Get current user profile
     */
    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.substring(7);
        UUID userId = jwtService.extractUserId(token);
        UserResponse response = authService.getUserProfile(userId);
        return ResponseEntity.ok(response);
    }

    /**
     * Validate token (for API Gateway)
     */
    @GetMapping("/validate")
    public ResponseEntity<TokenValidationResponse> validateToken(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.substring(7);
        boolean valid = jwtService.isTokenStructureValid(token) && !jwtService.isTokenExpired(token);

        if (valid) {
            return ResponseEntity.ok(TokenValidationResponse.builder()
                    .valid(true)
                    .userId(jwtService.extractUserId(token))
                    .username(jwtService.extractUsername(token))
                    .build());
        }

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(TokenValidationResponse.builder().valid(false).build());
    }

    /**
     * Token validation response
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class TokenValidationResponse {
        private boolean valid;
        private UUID userId;
        private String username;
    }
}

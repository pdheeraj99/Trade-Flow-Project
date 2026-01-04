package com.tradeflow.auth.controller;

import com.tradeflow.auth.dto.*;
import com.tradeflow.auth.security.JwtService;
import com.tradeflow.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.UUID;

/**
 * REST Controller for authentication endpoints.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Authentication", description = "User authentication and token management endpoints")
public class AuthController {

    private final AuthService authService;
    private final JwtService jwtService;

    @Value("${app.cookie.secure:true}")
    private boolean cookieSecure;

    /**
     * Register a new user
     */
    @Operation(summary = "Register new user", description = "Create a new user account and receive JWT tokens")
    @ApiResponse(responseCode = "201", description = "User registered successfully")
    @ApiResponse(responseCode = "400", description = "Invalid request data or user already exists")
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request, HttpServletResponse response) {
        log.info("Registration request for: {}", request.getUsername());
        AuthResponse authResponse = authService.register(request);
        setTokenCookies(response, authResponse);
        return ResponseEntity.status(HttpStatus.CREATED).body(authResponse);
    }

    /**
     * Login and get tokens
     */
    @Operation(summary = "User login", description = "Authenticate user and receive JWT tokens")
    @ApiResponse(responseCode = "200", description = "Login successful")
    @ApiResponse(responseCode = "401", description = "Invalid credentials")
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request, HttpServletResponse response) {
        log.info("Login request for: {}", request.getUsernameOrEmail());
        AuthResponse authResponse = authService.login(request);
        setTokenCookies(response, authResponse);
        return ResponseEntity.ok(authResponse);
    }

    /**
     * Refresh access token
     */
    @Operation(summary = "Refresh access token", description = "Get new access token using refresh token")
    @ApiResponse(responseCode = "200", description = "Token refreshed successfully")
    @ApiResponse(responseCode = "401", description = "Invalid or expired refresh token")
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refreshToken(@Valid @RequestBody RefreshTokenRequest request, HttpServletResponse response) {
        log.debug("Token refresh request");
        AuthResponse authResponse = authService.refreshToken(request);
        setTokenCookies(response, authResponse);
        return ResponseEntity.ok(authResponse);
    }

    /**
     * Logout - revoke all refresh tokens and clear cookies
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(name = "accessToken", required = false) String accessToken,
            HttpServletResponse response) {
        
        if (accessToken != null) {
            try {
                if (!jwtService.isTokenStructureValid(accessToken) || jwtService.isTokenExpired(accessToken)) {
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
                }
                if (!"access".equals(jwtService.extractTokenType(accessToken))) {
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
                }
                UUID userId = jwtService.extractUserId(accessToken);
                authService.logout(userId);
            } catch (Exception ex) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
        }
        
        clearTokenCookies(response);
        return ResponseEntity.noContent().build();
    }

    /**
     * Get current user profile
     */
    @Operation(summary = "Get current user", description = "Retrieve authenticated user profile information")
    @ApiResponse(responseCode = "200", description = "User profile retrieved successfully")
    @ApiResponse(responseCode = "401", description = "Unauthorized - invalid or missing token")
    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authHeader,
            @CookieValue(name = "accessToken", required = false) String accessCookie) {
        try {
            String token = extractToken(authHeader, accessCookie);
            if (token == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            if (!jwtService.isTokenStructureValid(token) || jwtService.isTokenExpired(token)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            if (!"access".equals(jwtService.extractTokenType(token))) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            UUID userId = jwtService.extractUserId(token);
            UserResponse response = authService.getUserProfile(userId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.warn("Failed to resolve /me token: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    /**
     * Validate token (for API Gateway)
     */
    @GetMapping("/validate")
    public ResponseEntity<TokenValidationResponse> validateToken(@RequestHeader(value = "Authorization", required = false) String authHeader,
                                                               @CookieValue(name = "accessToken", required = false) String cookieToken) {
        String token = null;
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);
        } else if (cookieToken != null) {
            token = cookieToken;
        }

        if (token == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(TokenValidationResponse.builder().valid(false).build());
        }

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

    private void setTokenCookies(HttpServletResponse response, AuthResponse authResponse) {
        ResponseCookie accessCookie = ResponseCookie.from("accessToken", authResponse.getAccessToken())
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite("Strict")
                .path("/")
                .maxAge(Duration.ofMillis(authResponse.getAccessTokenExpiresIn()))
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, accessCookie.toString());

        ResponseCookie refreshCookie = ResponseCookie.from("refreshToken", authResponse.getRefreshToken())
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite("Strict")
                .path("/")
                .maxAge(Duration.ofMillis(authResponse.getRefreshTokenExpiresIn()))
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());
    }

    private void clearTokenCookies(HttpServletResponse response) {
        ResponseCookie accessCookie = ResponseCookie.from("accessToken", "")
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite("Strict")
                .path("/")
                .maxAge(0)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, accessCookie.toString());

        ResponseCookie refreshCookie = ResponseCookie.from("refreshToken", "")
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite("Strict")
                .path("/")
                .maxAge(0)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());
    }

    private String extractToken(String authHeader, String cookieToken) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        if (cookieToken != null && !cookieToken.isBlank()) {
            return cookieToken;
        }
        return null;
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

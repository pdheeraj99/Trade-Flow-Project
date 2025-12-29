package com.tradeflow.auth.controller;

import com.tradeflow.auth.dto.*;
import com.tradeflow.auth.security.JwtService;
import com.tradeflow.auth.service.AuthService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request, HttpServletResponse response) {
        log.info("Registration request for: {}", request.getUsername());
        AuthResponse authResponse = authService.register(request);
        setTokenCookies(response, authResponse);
        return ResponseEntity.status(HttpStatus.CREATED).body(authResponse);
    }

    /**
     * Login and get tokens
     */
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
            UUID userId = jwtService.extractUserId(accessToken);
            authService.logout(userId);
        }
        
        clearTokenCookies(response);
        return ResponseEntity.noContent().build();
    }

    /**
     * Get current user profile
     */
    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser(@CookieValue(name = "accessToken") String accessToken) {
        UUID userId = jwtService.extractUserId(accessToken);
        UserResponse response = authService.getUserProfile(userId);
        return ResponseEntity.ok(response);
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
        // Access Token Cookie
        Cookie accessCookie = new Cookie("accessToken", authResponse.getAccessToken());
        accessCookie.setHttpOnly(true);
        accessCookie.setSecure(false); // Set to true in production with HTTPS
        accessCookie.setPath("/");
        accessCookie.setMaxAge((int) (authResponse.getAccessTokenExpiresIn() / 1000));
        response.addCookie(accessCookie);

        // Refresh Token Cookie
        Cookie refreshCookie = new Cookie("refreshToken", authResponse.getRefreshToken());
        refreshCookie.setHttpOnly(true);
        refreshCookie.setSecure(false); // Set to true in production with HTTPS
        refreshCookie.setPath("/");
        refreshCookie.setMaxAge((int) (authResponse.getRefreshTokenExpiresIn() / 1000));
        response.addCookie(refreshCookie);
    }

    private void clearTokenCookies(HttpServletResponse response) {
        Cookie accessCookie = new Cookie("accessToken", null);
        accessCookie.setHttpOnly(true);
        accessCookie.setPath("/");
        accessCookie.setMaxAge(0);
        response.addCookie(accessCookie);

        Cookie refreshCookie = new Cookie("refreshToken", null);
        refreshCookie.setHttpOnly(true);
        refreshCookie.setPath("/");
        refreshCookie.setMaxAge(0);
        response.addCookie(refreshCookie);
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

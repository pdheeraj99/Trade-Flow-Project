package com.tradeflow.auth.service;

import com.tradeflow.auth.dto.*;
import com.tradeflow.auth.entity.RefreshToken;
import com.tradeflow.auth.entity.Role;
import com.tradeflow.auth.entity.User;
import com.tradeflow.auth.exception.InvalidRefreshTokenException;
import com.tradeflow.auth.exception.UserAlreadyExistsException;
import com.tradeflow.auth.repository.RefreshTokenRepository;
import com.tradeflow.auth.repository.RoleRepository;
import com.tradeflow.auth.repository.UserRepository;
import com.tradeflow.auth.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Authentication Service handling registration, login, and token refresh.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    /**
     * Register a new user
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        log.info("Registering new user: {}", request.getUsername());

        // Check if username exists
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new UserAlreadyExistsException("username", request.getUsername());
        }

        // Check if email exists
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new UserAlreadyExistsException("email", request.getEmail());
        }

        // Get or create USER role
        Role userRole = roleRepository.findByName(Role.USER)
                .orElseGet(() -> roleRepository.save(Role.builder()
                        .name(Role.USER)
                        .description("Standard user role")
                        .build()));

        // Create user
        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .enabled(true)
                .accountNonLocked(true)
                .build();
        user.addRole(userRole);
        user = userRepository.save(user);

        log.info("User registered successfully: {} (ID: {})", user.getUsername(), user.getUserId());

        // Generate tokens
        return generateAuthResponse(user);
    }

    /**
     * Authenticate user and return tokens
     */
    @Transactional
    public AuthResponse login(LoginRequest request) {
        log.info("Login attempt for: {}", request.getUsernameOrEmail());

        // Authenticate using Spring Security
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsernameOrEmail(),
                        request.getPassword()));

        // Get user from database
        User user = userRepository.findByUsernameOrEmail(request.getUsernameOrEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Update last login
        user.setLastLoginAt(Instant.now());
        userRepository.save(user);

        log.info("User logged in successfully: {}", user.getUsername());

        // Generate tokens
        return generateAuthResponse(user);
    }

    /**
     * Refresh access token using refresh token
     */
    @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        log.debug("Refreshing token");

        // Find and validate refresh token
        RefreshToken refreshToken = refreshTokenRepository
                .findValidToken(request.getRefreshToken(), Instant.now())
                .orElseThrow(() -> new InvalidRefreshTokenException());

        User user = refreshToken.getUser();

        // Revoke old refresh token (rotation)
        refreshToken.setRevoked(true);
        refreshTokenRepository.save(refreshToken);

        log.info("Token refreshed for user: {}", user.getUsername());

        // Generate new tokens
        return generateAuthResponse(user);
    }

    /**
     * Logout - revoke all refresh tokens for user
     */
    @Transactional
    public void logout(UUID userId) {
        log.info("Logging out user: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        int revokedCount = refreshTokenRepository.revokeAllUserTokens(user);
        log.info("Revoked {} refresh tokens for user {}", revokedCount, userId);
    }

    /**
     * Generate auth response with new tokens
     */
    private AuthResponse generateAuthResponse(User user) {
        // Create UserDetails for token generation
        UserDetails userDetails = org.springframework.security.core.userdetails.User.builder()
                .username(user.getUsername())
                .password(user.getPasswordHash())
                .authorities(user.getRoles().stream()
                        .map(role -> "ROLE_" + role.getName())
                        .toArray(String[]::new))
                .build();

        // Generate JWT tokens
        String accessToken = jwtService.generateAccessToken(userDetails, user.getUserId());
        String refreshTokenString = jwtService.generateRefreshToken(userDetails, user.getUserId());

        // Store refresh token in database
        RefreshToken refreshToken = RefreshToken.builder()
                .token(refreshTokenString)
                .user(user)
                .expiresAt(Instant.now().plusMillis(jwtService.getRefreshTokenExpiration()))
                .revoked(false)
                .build();
        refreshTokenRepository.save(refreshToken);

        return AuthResponse.builder()
                .userId(user.getUserId())
                .username(user.getUsername())
                .email(user.getEmail())
                .roles(user.getRoles().stream()
                        .map(Role::getName)
                        .collect(Collectors.toList()))
                .accessToken(accessToken)
                .refreshToken(refreshTokenString)
                .accessTokenExpiresIn(jwtService.getAccessTokenExpiration())
                .refreshTokenExpiresIn(jwtService.getRefreshTokenExpiration())
                .tokenType("Bearer")
                .build();
    }

    /**
     * Get user profile by ID
     */
    @Transactional(readOnly = true)
    public UserResponse getUserProfile(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return UserResponse.builder()
                .userId(user.getUserId())
                .username(user.getUsername())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .roles(user.getRoles().stream()
                        .map(Role::getName)
                        .collect(Collectors.toList()))
                .enabled(user.isEnabled())
                .build();
    }
}

package com.tradeflow.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * Response DTO for authentication (login/register/refresh)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

    private UUID userId;
    private String username;
    private String email;
    private List<String> roles;
    private String accessToken;
    private String refreshToken;
    private long accessTokenExpiresIn; // milliseconds
    private long refreshTokenExpiresIn; // milliseconds
    private String tokenType;
}

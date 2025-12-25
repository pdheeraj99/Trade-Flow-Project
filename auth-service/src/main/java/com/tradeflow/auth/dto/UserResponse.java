package com.tradeflow.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * Response DTO for user profile
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {

    private UUID userId;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private List<String> roles;
    private boolean enabled;
}

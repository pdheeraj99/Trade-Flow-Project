package com.tradeflow.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for user login
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Login request with username/email and password")
public class LoginRequest {

    @Schema(description = "Username or email address", example = "john_doe", required = true)
    @NotBlank(message = "Username or email is required")
    private String usernameOrEmail;

    @Schema(description = "User password", example = "SecurePass123!", required = true)
    @NotBlank(message = "Password is required")
    private String password;
}

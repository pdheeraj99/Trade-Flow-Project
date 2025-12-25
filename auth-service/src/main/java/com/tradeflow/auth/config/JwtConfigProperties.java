package com.tradeflow.auth.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * JWT configuration properties.
 * All values can be overridden via application.yml or environment variables.
 */
@Configuration
@ConfigurationProperties(prefix = "jwt")
@Data
public class JwtConfigProperties {

    /**
     * Secret key for signing JWT tokens (min 256 bits for HS256)
     */
    private String secret = "tradeflow-super-secret-key-that-is-at-least-256-bits-long-for-hs256";

    /**
     * Access token expiration in milliseconds (default: 15 minutes)
     */
    private long accessTokenExpiration = 900000;

    /**
     * Refresh token expiration in milliseconds (default: 7 days)
     */
    private long refreshTokenExpiration = 604800000;

    /**
     * Token issuer
     */
    private String issuer = "tradeflow-auth";
}

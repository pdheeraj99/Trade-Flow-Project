package com.tradeflow.gateway.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Gateway configuration properties
 */
@Configuration
@ConfigurationProperties(prefix = "tradeflow.gateway")
@Data
public class GatewayConfigProperties {

    /**
     * JWT configuration
     */
    private Jwt jwt = new Jwt();

    /**
     * Rate limiting configuration
     */
    private RateLimiting rateLimiting = new RateLimiting();

    /**
     * CORS configuration
     */
    private Cors cors = new Cors();

    @Data
    public static class Jwt {
        private String secret;
        private List<String> publicPaths = List.of(
                "/api/auth/login",
                "/api/auth/register",
                "/api/auth/refresh",
                "/api/market/**",
                "/ws/**",
                "/actuator/**");
    }

    @Data
    public static class RateLimiting {
        private int requestsPerSecond = 10;
        private int burstCapacity = 20;
    }

    @Data
    public static class Cors {
        private List<String> allowedOrigins = List.of("http://localhost:3000", "http://localhost:5173");
        private List<String> allowedMethods = List.of("GET", "POST", "PUT", "DELETE", "OPTIONS");
        private List<String> allowedHeaders = List.of("*");
        private boolean allowCredentials = true;
    }
}

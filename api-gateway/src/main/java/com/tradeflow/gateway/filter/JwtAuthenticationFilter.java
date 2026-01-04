package com.tradeflow.gateway.filter;

import com.tradeflow.gateway.config.GatewayConfigProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Objects;
import java.util.Optional;

/**
 * Global JWT Authentication Filter for API Gateway.
 * Validates JWT tokens and forwards user info to downstream services.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    private final GatewayConfigProperties config;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getPath().value();

        // Skip authentication for public paths
        if (isPublicPath(path)) {
            log.debug("Skipping JWT validation for public path: {}", path);
            return chain.filter(exchange);
        }

        // Try to get token from Authorization header or cookies
        String token = extractToken(exchange);

        if (token == null) {
            log.warn("Missing or invalid Authorization header or cookie for path: {}", path);
            return onError(exchange, "Missing or invalid authentication token", HttpStatus.UNAUTHORIZED);
        }

        try {
            // Validate token
            Claims claims = validateToken(token);

            // Add user info to request headers for downstream services
            String userId = Objects.requireNonNull(claims.get("userId", String.class), "JWT missing userId claim");
            String username = Objects.requireNonNull(claims.getSubject(), "JWT missing subject claim");

            String tokenType = claims.get("type", String.class);
            if (!"access".equals(tokenType)) {
                log.warn("Rejecting token with invalid type: {}", tokenType);
                return onError(exchange, "Invalid token type", HttpStatus.UNAUTHORIZED);
            }

            ServerHttpRequest modifiedRequest = request.mutate()
                    .header("X-User-Id", userId)
                    .header("X-Username", username)
                    .build();

            return chain.filter(exchange.mutate().request(modifiedRequest).build());

        } catch (Exception e) {
            log.warn("JWT validation failed: {}", e.getMessage());
            return onError(exchange, "Invalid or expired token", HttpStatus.UNAUTHORIZED);
        }
    }

    private String extractToken(ServerWebExchange exchange) {
        // 1. Try Authorization Header
        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }

        // 2. Try Cookie
        return Optional.ofNullable(exchange.getRequest().getCookies().getFirst("accessToken"))
                .map(HttpCookie::getValue)
                .orElse(null);
    }

    /**
     * Check if path is public (no auth required)
     */
    private boolean isPublicPath(String path) {
        Objects.requireNonNull(path, "Request path must not be null");
        return config.getJwt().getPublicPaths().stream()
                .anyMatch(pattern -> pattern != null && pathMatcher.match(pattern, path));
    }

    /**
     * Validate JWT token and return claims
     */
    private Claims validateToken(String token) {
        SecretKey key = Keys.hmacShaKeyFor(
                config.getJwt().getSecret().getBytes(StandardCharsets.UTF_8));

        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        // Check expiration
        if (claims.getExpiration().before(new Date())) {
            throw new RuntimeException("Token expired");
        }

        return claims;
    }

    /**
     * Return error response
     */
    private Mono<Void> onError(ServerWebExchange exchange, String message, HttpStatus status) {
        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().add("Content-Type", "application/json");
        String body = String.format("{\"error\":\"%s\",\"status\":%d}", message, status.value());
        byte[] bytes = Objects.requireNonNull(body.getBytes(StandardCharsets.UTF_8));
        DataBuffer buffer = Objects.requireNonNull(exchange.getResponse()
                .bufferFactory()
                .wrap(bytes), "Failed to create response buffer");
        org.reactivestreams.Publisher<? extends DataBuffer> publisher = Objects.requireNonNull(Mono.just(buffer));
        return exchange.getResponse().writeWith(publisher);
    }

    @Override
    public int getOrder() {
        return -100; // High priority
    }
}

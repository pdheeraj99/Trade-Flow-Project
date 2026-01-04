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
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * Enforces JWT authentication on WebSocket upgrade requests.
 * Accepts token via Sec-WebSocket-Protocol header or query param "token".
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketAuthenticationFilter implements GlobalFilter, Ordered {

    private final GatewayConfigProperties config;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String upgrade = request.getHeaders().getFirst("Upgrade");
        if (upgrade == null || !"websocket".equalsIgnoreCase(upgrade)) {
            return chain.filter(exchange); // non-WS requests handled elsewhere
        }

        String token = resolveWebSocketToken(request);
        if (token == null) {
            log.warn("Missing WebSocket token for path {}", request.getPath().value());
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        try {
            Claims claims = validate(token);
            String tokenType = claims.get("type", String.class);
            if (!"access".equals(tokenType)) {
                throw new IllegalStateException("Invalid token type");
            }

            ServerHttpRequest mutated = request.mutate()
                    .header("X-User-Id", claims.get("userId", String.class))
                    .header("X-Username", claims.getSubject())
                    .build();
            return chain.filter(exchange.mutate().request(mutated).build());
        } catch (Exception ex) {
            log.warn("WebSocket JWT validation failed: {}", ex.getMessage());
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }
    }

    private String resolveWebSocketToken(ServerHttpRequest request) {
        // Preferred: Sec-WebSocket-Protocol: Bearer,<token>
        String protocolHeader = request.getHeaders().getFirst("Sec-WebSocket-Protocol");
        if (protocolHeader != null && protocolHeader.startsWith("Bearer,")) {
            return protocolHeader.substring("Bearer,".length()).trim();
        }

        // Fallback: query parameter token
        MultiValueMap<String, String> queryParams = request.getQueryParams();
        if (queryParams.containsKey("token")) {
            return queryParams.getFirst("token");
        }
        return null;
    }

    private Claims validate(String token) {
        SecretKey key = Keys.hmacShaKeyFor(
                config.getJwt().getSecret().getBytes(StandardCharsets.UTF_8));

        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        if (claims.getExpiration() != null && claims.getExpiration().getTime() < System.currentTimeMillis()) {
            throw new IllegalStateException("Token expired");
        }
        Objects.requireNonNull(claims.get("userId", String.class), "Missing userId");
        Objects.requireNonNull(claims.getSubject(), "Missing subject");
        return claims;
    }

    @Override
    public int getOrder() {
        return -90; // run after JWT filter ordering but before routing
    }
}

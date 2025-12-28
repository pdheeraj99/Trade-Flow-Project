package com.tradeflow.gateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

import java.net.InetAddress;
import java.net.InetSocketAddress;

/**
 * Rate limiting configuration using Redis
 */
@Configuration
public class RateLimiterConfig {

    /**
     * Rate limit by user ID (from JWT) or IP address
     */
    @Bean
    public KeyResolver userKeyResolver() {
        return exchange -> {
            // Try to get user ID from header (set by JWT filter)
            String userId = exchange.getRequest().getHeaders().getFirst("X-User-Id");
            if (userId != null && !userId.isEmpty()) {
                return Mono.just(userId);
            }

            // Fall back to IP address
            InetSocketAddress remoteAddress = exchange.getRequest().getRemoteAddress();
            if (remoteAddress == null) {
                return Mono.just("anonymous");
            }

            InetAddress address = remoteAddress.getAddress();
            String ip = address != null ? address.getHostAddress() : null;
            return Mono.just(ip != null ? ip : "anonymous");
        };
    }
}

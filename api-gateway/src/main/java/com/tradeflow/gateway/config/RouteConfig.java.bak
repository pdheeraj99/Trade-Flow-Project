package com.tradeflow.gateway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Route configuration for API Gateway.
 * Defines routing rules to downstream microservices.
 */
@Configuration
public class RouteConfig {

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                // Auth Service Routes
                .route("auth-service", r -> r
                        .path("/api/auth/**")
                        .uri("lb://auth-service"))

                // Wallet Service Routes
                .route("wallet-service", r -> r
                        .path("/api/wallet/**", "/api/wallets/**")
                        .uri("lb://wallet-service"))

                // OMS Service Routes
                .route("oms-service", r -> r
                        .path("/api/orders/**")
                        .uri("lb://oms-service"))

                // Matching Engine Routes (monitoring only)
                .route("matching-engine", r -> r
                        .path("/api/matching/**")
                        .uri("lb://matching-engine"))

                // Market Data Service Routes
                .route("market-data-service", r -> r
                        .path("/api/market/**")
                        .uri("lb://market-data-service"))

                // Market Data WebSocket Routes
                .route("market-data-ws", r -> r
                        .path("/ws/market/**")
                        .uri("lb://market-data-service"))

                .build();
    }
}

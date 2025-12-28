package com.tradeflow.gateway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RouteConfig {

    @Bean
    public RouteLocator gatewayRoutes(RouteLocatorBuilder builder) {
        return builder.routes()
            // WebSocket route for market data
            .route("market_websocket", r -> r.path("/ws/market/**")
                .uri("ws://localhost:8085"))
            
            // WebSocket route for order updates
            .route("order_updates_websocket", r -> r.path("/ws/orders/**")
                .uri("ws://localhost:8083"))
            
            // WebSocket route for wallet balance updates
            .route("wallet_websocket", r -> r.path("/ws/wallet/**")
                .uri("ws://localhost:8082"))
            
            // HTTP routing for OMS order APIs
            .route("oms_orders", r -> r.path("/api/orders/**")
                .uri("http://localhost:8083"))
            
            // HTTP routing for Wallet APIs
            .route("wallet_apis", r -> r.path("/api/wallet/**")
                .uri("http://localhost:8082"))
            
            // HTTP routing for Market Data APIs
            .route("market_apis", r -> r.path("/api/market/**")
                .uri("http://localhost:8085"))
            
            // HTTP routing for Auth APIs
            .route("auth_apis", r -> r.path("/api/auth/**")
                .uri("http://localhost:8081"))
            
            .build();
    }
}

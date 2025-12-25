package com.tradeflow.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

/**
 * CORS configuration for API Gateway
 */
@Configuration
public class CorsConfig {

    private final GatewayConfigProperties config;

    public CorsConfig(GatewayConfigProperties config) {
        this.config = config;
    }

    @Bean
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration corsConfig = new CorsConfiguration();

        // Set allowed origins
        config.getCors().getAllowedOrigins().forEach(corsConfig::addAllowedOrigin);

        // Set allowed methods
        config.getCors().getAllowedMethods().forEach(corsConfig::addAllowedMethod);

        // Set allowed headers
        config.getCors().getAllowedHeaders().forEach(corsConfig::addAllowedHeader);

        // Allow credentials
        corsConfig.setAllowCredentials(config.getCors().isAllowCredentials());

        // Expose headers
        corsConfig.addExposedHeader("Authorization");
        corsConfig.addExposedHeader("X-User-Id");

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfig);

        return new CorsWebFilter(source);
    }
}

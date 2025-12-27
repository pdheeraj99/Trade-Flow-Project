package com.tradeflow.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * TradeFlow Authentication Service
 * Handles user registration, login, and JWT token management
 */
@SpringBootApplication
@EntityScan(basePackages = "com.tradeflow.auth.entity")
@EnableJpaRepositories(basePackages = "com.tradeflow.auth.repository")
public class AuthServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuthServiceApplication.class, args);
    }
}

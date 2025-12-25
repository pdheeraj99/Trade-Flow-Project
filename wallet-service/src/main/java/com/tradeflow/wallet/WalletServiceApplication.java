package com.tradeflow.wallet;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * TradeFlow Wallet Service
 * Handles double-entry ledger, balance management, and fund reservation for
 * Saga
 */
@SpringBootApplication
public class WalletServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(WalletServiceApplication.class, args);
    }
}

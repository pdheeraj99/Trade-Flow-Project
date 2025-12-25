package com.tradeflow.wallet.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;

/**
 * Wallet configuration properties - externalized for production flexibility.
 * All values can be overridden via application.yml or environment variables.
 */
@Configuration
@ConfigurationProperties(prefix = "tradeflow.wallet")
@Data
public class WalletConfigProperties {

    /**
     * Faucet configuration
     */
    private Faucet faucet = new Faucet();

    /**
     * Precision configuration for BigDecimal operations
     */
    private Precision precision = new Precision();

    @Data
    public static class Faucet {
        /**
         * Amount credited on faucet claim
         */
        private BigDecimal amount = new BigDecimal("10000.00");

        /**
         * Currency for faucet deposits
         */
        private String currency = "USD";

        /**
         * Cooldown period in seconds between faucet claims
         */
        private long cooldownSeconds = 3600; // 1 hour
    }

    @Data
    public static class Precision {
        /**
         * Decimal scale for currency amounts (8 for crypto like BTC satoshi)
         */
        private int scale = 8;
    }
}

package com.tradeflow.marketdata.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for Market Data Service
 */
@Configuration
@ConfigurationProperties(prefix = "tradeflow.market-data")
@Data
public class MarketDataConfigProperties {

    /**
     * CoinGecko API configuration
     */
    private CoinGecko coinGecko = new CoinGecko();

    /**
     * Cache configuration
     */
    private Cache cache = new Cache();

    /**
     * WebSocket configuration
     */
    private WebSocket webSocket = new WebSocket();

    @Data
    public static class CoinGecko {
        /**
         * CoinGecko API base URL
         */
        private String baseUrl = "https://api.coingecko.com/api/v3";

        /**
         * API key (optional, for pro tier)
         */
        private String apiKey = "";

        /**
         * Request timeout in milliseconds
         */
        private long timeoutMs = 10000;

        /**
         * Rate limit delay between requests in milliseconds
         */
        private long rateLimitDelayMs = 1500; // CoinGecko free tier: 10-30 calls/min
    }

    @Data
    public static class Cache {
        /**
         * Ticker cache TTL in seconds
         */
        private long tickerTtlSeconds = 10;

        /**
         * Market data cache TTL in seconds
         */
        private long marketDataTtlSeconds = 60;
    }

    @Data
    public static class WebSocket {
        /**
         * Broadcast interval in milliseconds
         */
        private long broadcastIntervalMs = 1000;
    }
}

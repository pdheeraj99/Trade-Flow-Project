package com.tradeflow.marketdata;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * TradeFlow Market Data Service
 * Real-time market data from CoinGecko API with Redis caching and WebSocket
 * broadcast
 */
@SpringBootApplication
@EnableScheduling
public class MarketDataServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(MarketDataServiceApplication.class, args);
    }
}

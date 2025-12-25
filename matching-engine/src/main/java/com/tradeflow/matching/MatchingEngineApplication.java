package com.tradeflow.matching;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * TradeFlow Matching Engine
 * High-performance price-time priority order matching using Platform Threads
 * (CPU-bound)
 */
@SpringBootApplication
public class MatchingEngineApplication {

    public static void main(String[] args) {
        SpringApplication.run(MatchingEngineApplication.class, args);
    }
}

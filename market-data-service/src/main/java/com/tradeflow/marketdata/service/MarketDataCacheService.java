package com.tradeflow.marketdata.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradeflow.marketdata.config.MarketDataConfigProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

/**
 * Redis cache service for market data.
 * Implements TTL-based caching for ticker and market data.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MarketDataCacheService {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final MarketDataConfigProperties config;

    private static final String TICKER_PREFIX = "ticker:";
    private static final String MARKET_PREFIX = "market:";

    /**
     * Cache ticker data
     */
    public void cacheTicker(String symbol, Object tickerData) {
        String key = TICKER_PREFIX + symbol.toUpperCase();
        try {
            String json = objectMapper.writeValueAsString(tickerData);
            redisTemplate.opsForValue().set(key, json,
                    Duration.ofSeconds(config.getCache().getTickerTtlSeconds()));
            log.debug("Cached ticker for {}", symbol);
        } catch (JsonProcessingException e) {
            log.error("Error serializing ticker data for {}", symbol, e);
        }
    }

    /**
     * Get cached ticker data
     */
    public <T> Optional<T> getCachedTicker(String symbol, Class<T> type) {
        String key = TICKER_PREFIX + symbol.toUpperCase();
        String json = redisTemplate.opsForValue().get(key);

        if (json == null) {
            return Optional.empty();
        }

        try {
            return Optional.of(objectMapper.readValue(json, type));
        } catch (JsonProcessingException e) {
            log.error("Error deserializing ticker data for {}", symbol, e);
            return Optional.empty();
        }
    }

    /**
     * Cache market data
     */
    public void cacheMarketData(String coinId, Object marketData) {
        String key = MARKET_PREFIX + coinId.toLowerCase();
        try {
            String json = objectMapper.writeValueAsString(marketData);
            redisTemplate.opsForValue().set(key, json,
                    Duration.ofSeconds(config.getCache().getMarketDataTtlSeconds()));
            log.debug("Cached market data for {}", coinId);
        } catch (JsonProcessingException e) {
            log.error("Error serializing market data for {}", coinId, e);
        }
    }

    /**
     * Get cached market data
     */
    public <T> Optional<T> getCachedMarketData(String coinId, Class<T> type) {
        String key = MARKET_PREFIX + coinId.toLowerCase();
        String json = redisTemplate.opsForValue().get(key);

        if (json == null) {
            return Optional.empty();
        }

        try {
            return Optional.of(objectMapper.readValue(json, type));
        } catch (JsonProcessingException e) {
            log.error("Error deserializing market data for {}", coinId, e);
            return Optional.empty();
        }
    }

    /**
     * Invalidate ticker cache
     */
    public void invalidateTicker(String symbol) {
        String key = TICKER_PREFIX + symbol.toUpperCase();
        redisTemplate.delete(key);
        log.debug("Invalidated ticker cache for {}", symbol);
    }

    /**
     * Check if ticker is stale (not in cache or expired)
     */
    public boolean isTickerStale(String symbol) {
        String key = TICKER_PREFIX + symbol.toUpperCase();
        return Boolean.FALSE.equals(redisTemplate.hasKey(key));
    }
}

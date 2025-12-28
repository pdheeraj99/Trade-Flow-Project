package com.tradeflow.marketdata.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradeflow.marketdata.config.MarketDataConfigProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Objects;
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
        String safeSymbol = Objects.requireNonNull(symbol, "symbol must not be null");
        String key = TICKER_PREFIX + safeSymbol.toUpperCase();
        try {
            String json = Objects.requireNonNull(
                    objectMapper.writeValueAsString(tickerData),
                    "Serialized ticker data must not be null");
            Duration ttl = Objects.requireNonNull(
                    Duration.ofSeconds(config.getCache().getTickerTtlSeconds()),
                    "Ticker TTL must not be null");
            redisTemplate.opsForValue().set(key, json, ttl);
            log.debug("Cached ticker for {}", safeSymbol);
        } catch (JsonProcessingException e) {
            log.error("Error serializing ticker data for {}", safeSymbol, e);
        }
    }

    /**
     * Get cached ticker data
     */
    public <T> Optional<T> getCachedTicker(String symbol, Class<T> type) {
        String safeSymbol = Objects.requireNonNull(symbol, "symbol must not be null");
        String key = TICKER_PREFIX + safeSymbol.toUpperCase();
        String json = redisTemplate.opsForValue().get(key);

        if (json == null) {
            return Optional.empty();
        }

        try {
            return Optional.of(objectMapper.readValue(json, type));
        } catch (JsonProcessingException e) {
            log.error("Error deserializing ticker data for {}", safeSymbol, e);
            return Optional.empty();
        }
    }

    /**
     * Cache market data
     */
    public void cacheMarketData(String coinId, Object marketData) {
        String safeCoinId = Objects.requireNonNull(coinId, "coinId must not be null");
        String key = MARKET_PREFIX + safeCoinId.toLowerCase();
        try {
            String json = Objects.requireNonNull(
                    objectMapper.writeValueAsString(marketData),
                    "Serialized market data must not be null");
            Duration ttl = Objects.requireNonNull(
                    Duration.ofSeconds(config.getCache().getMarketDataTtlSeconds()),
                    "Market data TTL must not be null");
            redisTemplate.opsForValue().set(key, json, ttl);
            log.debug("Cached market data for {}", safeCoinId);
        } catch (JsonProcessingException e) {
            log.error("Error serializing market data for {}", safeCoinId, e);
        }
    }

    /**
     * Get cached market data
     */
    public <T> Optional<T> getCachedMarketData(String coinId, Class<T> type) {
        String safeCoinId = Objects.requireNonNull(coinId, "coinId must not be null");
        String key = MARKET_PREFIX + safeCoinId.toLowerCase();
        String json = redisTemplate.opsForValue().get(key);

        if (json == null) {
            return Optional.empty();
        }

        try {
            return Optional.of(objectMapper.readValue(json, type));
        } catch (JsonProcessingException e) {
            log.error("Error deserializing market data for {}", safeCoinId, e);
            return Optional.empty();
        }
    }

    /**
     * Invalidate ticker cache
     */
    public void invalidateTicker(String symbol) {
        String safeSymbol = Objects.requireNonNull(symbol, "symbol must not be null");
        String key = TICKER_PREFIX + safeSymbol.toUpperCase();
        redisTemplate.delete(key);
        log.debug("Invalidated ticker cache for {}", safeSymbol);
    }

    /**
     * Check if ticker is stale (not in cache or expired)
     */
    public boolean isTickerStale(String symbol) {
        String safeSymbol = Objects.requireNonNull(symbol, "symbol must not be null");
        String key = TICKER_PREFIX + safeSymbol.toUpperCase();
        return Boolean.FALSE.equals(redisTemplate.hasKey(key));
    }
}

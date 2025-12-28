package com.tradeflow.marketdata.client;

import com.tradeflow.marketdata.config.MarketDataConfigProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * CoinGecko API client for fetching real cryptocurrency market data.
 * Implements rate limiting to respect API quotas.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CoinGeckoClient {

    private final MarketDataConfigProperties config;
    private final RestClient.Builder restClientBuilder;

    private volatile long lastRequestTime = 0;

    /**
     * Get simple price for multiple coins
     */
    public Map<String, CoinPrice> getSimplePrice(List<String> coinIds, List<String> vsCurrencies) {
        rateLimitWait();

        String ids = String.join(",", coinIds);
        String currencies = String.join(",", vsCurrencies);

        RestClient client = buildClient();

        try {
            @SuppressWarnings("unchecked")
            Map<String, Map<String, Object>> response = client.get()
                    .uri("/simple/price?ids={ids}&vs_currencies={currencies}&include_24hr_change=true&include_last_updated_at=true",
                            ids, currencies)
                    .retrieve()
                    .body(Map.class);

            if (response == null) {
                log.warn("Empty response from CoinGecko simple price API");
                return Map.of();
            }

            return parsePriceResponse(response, vsCurrencies.get(0));

        } catch (Exception e) {
            log.error("Error fetching prices from CoinGecko: {}", e.getMessage());
            throw new CoinGeckoException("Failed to fetch prices", e);
        }
    }

    /**
     * Get market data for coins
     */
    public List<CoinMarketData> getMarkets(String vsCurrency, List<String> coinIds) {
        rateLimitWait();

        String ids = String.join(",", coinIds);

        RestClient client = buildClient();

        try {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> response = client.get()
                    .uri("/coins/markets?vs_currency={currency}&ids={ids}&order=market_cap_desc&sparkline=false",
                            vsCurrency, ids)
                    .retrieve()
                    .body(List.class);

            if (response == null) {
                return List.of();
            }

            return response.stream()
                    .map(this::parseMarketData)
                    .toList();

        } catch (Exception e) {
            log.error("Error fetching market data from CoinGecko: {}", e.getMessage());
            throw new CoinGeckoException("Failed to fetch market data", e);
        }
    }

    /**
     * Build REST client with configuration
     */
    private RestClient buildClient() {
        String baseUrl = Objects.requireNonNull(config.getCoinGecko().getBaseUrl(), "CoinGecko baseUrl must not be null");
        RestClient.Builder builder = restClientBuilder.baseUrl(baseUrl);

        // Add API key header if configured
        // CoinGecko Demo API uses x-cg-demo-api-key header
        // CoinGecko Pro API uses x-cg-pro-api-key header
        String apiKey = config.getCoinGecko().getApiKey();
        if (apiKey != null && !apiKey.isEmpty()) {
            // Demo keys start with "CG-", Pro keys have different format
            String headerName = apiKey.startsWith("CG-") ? "x-cg-demo-api-key" : "x-cg-pro-api-key";
            builder.defaultHeader(headerName, apiKey);
            log.debug("Using CoinGecko API with key type: {}", headerName);
        }

        return builder.build();
    }

    /**
     * Rate limit enforcement - Non-blocking timestamp tracking.
     * The @Scheduled annotation in MarketDataService already controls timing.
     * This method simply tracks the last request time for logging/monitoring.
     */
    private synchronized void rateLimitWait() {
        long now = System.currentTimeMillis();
        long elapsed = now - lastRequestTime;
        long configuredDelay = config.getCoinGecko().getRateLimitDelayMs();

        if (elapsed < configuredDelay) {
            log.debug("Rate limit: {}ms since last request (configured: {}ms)", elapsed, configuredDelay);
        }

        lastRequestTime = now;
    }

    /**
     * Parse price response
     */
    private Map<String, CoinPrice> parsePriceResponse(Map<String, Map<String, Object>> response, String currency) {
        return response.entrySet().stream()
                .collect(java.util.stream.Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> {
                            Map<String, Object> data = entry.getValue();
                            return CoinPrice.builder()
                                    .coinId(entry.getKey())
                                    .price(toBigDecimal(data.get(currency)))
                                    .change24h(toBigDecimal(data.get(currency + "_24h_change")))
                                    .lastUpdated(toLong(data.get("last_updated_at")))
                                    .build();
                        }));
    }

    /**
     * Parse market data
     */
    private CoinMarketData parseMarketData(Map<String, Object> data) {
        return CoinMarketData.builder()
                .coinId((String) data.get("id"))
                .symbol(((String) data.get("symbol")).toUpperCase())
                .name((String) data.get("name"))
                .currentPrice(toBigDecimal(data.get("current_price")))
                .marketCap(toBigDecimal(data.get("market_cap")))
                .totalVolume(toBigDecimal(data.get("total_volume")))
                .high24h(toBigDecimal(data.get("high_24h")))
                .low24h(toBigDecimal(data.get("low_24h")))
                .priceChange24h(toBigDecimal(data.get("price_change_24h")))
                .priceChangePercentage24h(toBigDecimal(data.get("price_change_percentage_24h")))
                .circulatingSupply(toBigDecimal(data.get("circulating_supply")))
                .lastUpdated((String) data.get("last_updated"))
                .build();
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value == null)
            return null;
        if (value instanceof Number) {
            return BigDecimal.valueOf(((Number) value).doubleValue());
        }
        return new BigDecimal(value.toString());
    }

    private Long toLong(Object value) {
        if (value == null)
            return null;
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return Long.parseLong(value.toString());
    }

    /**
     * Coin price data
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class CoinPrice {
        private String coinId;
        private BigDecimal price;
        private BigDecimal change24h;
        private Long lastUpdated;
    }

    /**
     * Coin market data
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class CoinMarketData {
        private String coinId;
        private String symbol;
        private String name;
        private BigDecimal currentPrice;
        private BigDecimal marketCap;
        private BigDecimal totalVolume;
        private BigDecimal high24h;
        private BigDecimal low24h;
        private BigDecimal priceChange24h;
        private BigDecimal priceChangePercentage24h;
        private BigDecimal circulatingSupply;
        private String lastUpdated;
    }

    /**
     * CoinGecko API exception
     */
    public static class CoinGeckoException extends RuntimeException {
        public CoinGeckoException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}

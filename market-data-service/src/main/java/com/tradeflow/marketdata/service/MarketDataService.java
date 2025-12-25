package com.tradeflow.marketdata.service;

import com.tradeflow.common.constants.TradingPairs;
import com.tradeflow.marketdata.client.CoinGeckoClient;
import com.tradeflow.marketdata.dto.TickerResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Market Data Service providing real-time cryptocurrency prices.
 * Fetches data from CoinGecko API and caches in Redis.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MarketDataService {

    private final CoinGeckoClient coinGeckoClient;
    private final MarketDataCacheService cacheService;

    // In-memory latest prices for fast access
    private final Map<String, TickerResponse> latestTickers = new ConcurrentHashMap<>();

    // Supported coins mapping: symbol -> coinId
    private static final Map<String, String> COIN_ID_MAP = Map.of(
            "BTC", "bitcoin",
            "ETH", "ethereum",
            "USDT", "tether");

    /**
     * Get ticker for a trading pair
     */
    public TickerResponse getTicker(String symbol) {
        String upperSymbol = symbol.toUpperCase();

        // Check in-memory cache first
        TickerResponse cached = latestTickers.get(upperSymbol);
        if (cached != null && !isStale(cached)) {
            return cached;
        }

        // Check Redis cache
        Optional<TickerResponse> redisCached = cacheService.getCachedTicker(upperSymbol, TickerResponse.class);
        if (redisCached.isPresent() && !isStale(redisCached.get())) {
            latestTickers.put(upperSymbol, redisCached.get());
            return redisCached.get();
        }

        // Fetch fresh data
        return fetchAndCacheTicker(upperSymbol);
    }

    /**
     * Get all tickers
     */
    public List<TickerResponse> getAllTickers() {
        return TradingPairs.SUPPORTED_PAIRS.stream()
                .map(this::getTicker)
                .toList();
    }

    /**
     * Fetch and cache ticker data
     */
    private TickerResponse fetchAndCacheTicker(String symbol) {
        try {
            // Parse symbol (e.g., BTCUSDT -> BTC)
            String baseCurrency = TradingPairs.getBaseCurrency(symbol);
            String coinId = COIN_ID_MAP.getOrDefault(baseCurrency, baseCurrency.toLowerCase());

            // Fetch from CoinGecko
            Map<String, CoinGeckoClient.CoinPrice> prices = coinGeckoClient.getSimplePrice(
                    List.of(coinId),
                    List.of("usd"));

            CoinGeckoClient.CoinPrice coinPrice = prices.get(coinId);
            if (coinPrice == null) {
                log.warn("No price data for {}", symbol);
                return createStaleTicker(symbol);
            }

            // Get additional market data
            List<CoinGeckoClient.CoinMarketData> marketData = coinGeckoClient.getMarkets("usd", List.of(coinId));
            CoinGeckoClient.CoinMarketData market = marketData.isEmpty() ? null : marketData.get(0);

            TickerResponse ticker = TickerResponse.builder()
                    .symbol(symbol)
                    .coinId(coinId)
                    .price(coinPrice.getPrice())
                    .change24h(market != null ? market.getPriceChange24h() : null)
                    .changePercent24h(coinPrice.getChange24h())
                    .high24h(market != null ? market.getHigh24h() : null)
                    .low24h(market != null ? market.getLow24h() : null)
                    .volume24h(market != null ? market.getTotalVolume() : null)
                    .timestamp(Instant.now())
                    .stale(false)
                    .build();

            // Cache in Redis and memory
            cacheService.cacheTicker(symbol, ticker);
            latestTickers.put(symbol, ticker);

            log.info("Updated ticker for {}: {}", symbol, ticker.getPrice());
            return ticker;

        } catch (Exception e) {
            log.error("Error fetching ticker for {}: {}", symbol, e.getMessage());
            return createStaleTicker(symbol);
        }
    }

    /**
     * Scheduled task to refresh tickers
     */
    @Scheduled(fixedDelayString = "${tradeflow.market-data.refresh-interval-ms:30000}")
    public void refreshTickers() {
        log.debug("Refreshing tickers...");
        for (String symbol : TradingPairs.SUPPORTED_PAIRS) {
            try {
                fetchAndCacheTicker(symbol);
            } catch (Exception e) {
                log.error("Error refreshing ticker for {}", symbol, e);
            }
        }
    }

    /**
     * Check if ticker is stale
     */
    private boolean isStale(TickerResponse ticker) {
        if (ticker == null || ticker.getTimestamp() == null) {
            return true;
        }
        // Consider stale if older than 30 seconds
        return ticker.getTimestamp().plusSeconds(30).isBefore(Instant.now());
    }

    /**
     * Create a stale ticker response (when API fails)
     */
    private TickerResponse createStaleTicker(String symbol) {
        TickerResponse existing = latestTickers.get(symbol);
        if (existing != null) {
            return existing.toBuilder().stale(true).build();
        }
        return TickerResponse.builder()
                .symbol(symbol)
                .stale(true)
                .timestamp(Instant.now())
                .build();
    }

    /**
     * Get latest price for a symbol (for internal use)
     */
    public BigDecimal getLatestPrice(String symbol) {
        TickerResponse ticker = getTicker(symbol);
        return ticker != null ? ticker.getPrice() : null;
    }
}

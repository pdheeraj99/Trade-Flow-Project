package com.tradeflow.marketdata.service;

import com.tradeflow.common.constants.TradingPairs;
import com.tradeflow.marketdata.client.BinanceWebSocketClient;
import com.tradeflow.marketdata.client.CoinGeckoClient;
import com.tradeflow.marketdata.dto.TickerResponse;
import com.tradeflow.marketdata.websocket.MarketDataBroadcaster;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Market Data Service providing real-time cryptocurrency prices.
 * 
 * 🚀 PRIMARY DATA SOURCE: Binance WebSocket (real-time, sub-second updates)
 * ⚠️ FALLBACK DATA SOURCE: CoinGecko REST API (disabled by default, 30s
 * polling)
 * 
 * Architecture:
 * - BinanceWebSocketClient: Receives real-time ticker updates from Binance
 * - This service: Processes and broadcasts updates via WebSocket to frontend
 * - CoinGeckoClient: Available as fallback if Binance issues occur
 * 
 * Real-time flow:
 * Binance WebSocket → MarketDataService → MarketDataBroadcaster → Frontend
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MarketDataService {

    private final BinanceWebSocketClient binanceClient;
    private final CoinGeckoClient coinGeckoClient; // ⚠️ FALLBACK: Kept for emergency fail-over
    private final MarketDataCacheService cacheService;
    @Lazy
    private final MarketDataBroadcaster marketDataBroadcaster;

    // In-memory latest prices for fast access
    private final Map<String, TickerResponse> latestTickers = new ConcurrentHashMap<>();

    // Supported coins mapping: symbol -> coinId
    private static final Map<String, String> COIN_ID_MAP = Map.of(
            "BTC", "bitcoin",
            "ETH", "ethereum",
            "USDT", "tether");

    /**
     * Initialize Binance WebSocket listeners for real-time updates
     */
    @PostConstruct
    public void initializeBinanceWebSocket() {
        log.info("[Market Data] Initializing Binance WebSocket listeners...");

        // Register listener for BTCUSDT
        binanceClient.registerTickerListener("BTCUSDT", update -> {
            TickerResponse ticker = TickerResponse.builder()
                    .symbol("BTCUSDT")
                    .coinId("bitcoin")
                    .price(update.getPrice())
                    .change24h(update.getChange24h())
                    .changePercent24h(update.getChangePercent24h())
                    .high24h(update.getHigh24h())
                    .low24h(update.getLow24h())
                    .volume24h(update.getVolume24h())
                    .timestamp(Instant.ofEpochMilli(update.getEventTime()))
                    .stale(false)
                    .build();

            // Cache and broadcast
            latestTickers.put("BTCUSDT", ticker);
            cacheService.cacheTicker("BTCUSDT", ticker);
            marketDataBroadcaster.broadcastTickerUpdate(ticker);

            log.debug("[Real-time] BTCUSDT = ${} ({}%)",
                    ticker.getPrice(), ticker.getChangePercent24h());
        });

        // Register listener for ETHUSDT
        binanceClient.registerTickerListener("ETHUSDT", update -> {
            TickerResponse ticker = TickerResponse.builder()
                    .symbol("ETHUSDT")
                    .coinId("ethereum")
                    .price(update.getPrice())
                    .change24h(update.getChange24h())
                    .changePercent24h(update.getChangePercent24h())
                    .high24h(update.getHigh24h())
                    .low24h(update.getLow24h())
                    .volume24h(update.getVolume24h())
                    .timestamp(Instant.ofEpochMilli(update.getEventTime()))
                    .stale(false)
                    .build();

            // Cache and broadcast
            latestTickers.put("ETHUSDT", ticker);
            cacheService.cacheTicker("ETHUSDT", ticker);
            marketDataBroadcaster.broadcastTickerUpdate(ticker);

            log.debug("[Real-time] ETHUSDT = ${} ({}%)",
                    ticker.getPrice(), ticker.getChangePercent24h());
        });

        log.info("[Market Data] ✅ Binance WebSocket listeners ready!");
    }

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
     * ⚠️ DISABLED SCHEDULER - CoinGecko Fallback Mechanism
     * 
     * This scheduled task is DISABLED in favor of Binance WebSocket real-time
     * updates.
     * CoinGecko polling (30s interval) replaced with event-driven Binance WebSocket
     * (1s updates).
     * 
     * TO RE-ENABLE (emergency fallback):
     * 1. Uncomment @Scheduled annotation below
     * 2. Disable Binance: Comment out @PostConstruct in BinanceWebSocketClient
     * 3. Restart Market Data Service
     * 
     * Rate limiting: 5 seconds between calls to avoid CoinGecko 429 errors
     */
    // @Scheduled(fixedDelayString =
    // "${tradeflow.market-data.refresh-interval-ms:60000}")
    public void refreshTickers() {
        log.debug("Refreshing tickers...");
        for (String symbol : TradingPairs.SUPPORTED_PAIRS) {
            try {
                fetchAndCacheTicker(symbol);
                // Rate limit: wait 5 seconds between API calls to avoid 429
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("Ticker refresh interrupted");
                break;
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

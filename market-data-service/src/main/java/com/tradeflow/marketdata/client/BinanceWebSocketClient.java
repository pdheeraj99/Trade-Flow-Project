package com.tradeflow.marketdata.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradeflow.marketdata.config.MarketDataConfigProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.function.Consumer;

/**
 * Binance WebSocket client for real-time cryptocurrency market data.
 * Provides millisecond-level price updates via WebSocket streams.
 * 
 * NO API KEY REQUIRED for public market data streams.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class BinanceWebSocketClient {

    private final ObjectMapper objectMapper;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    private WebSocketSession session;
    private final Map<String, Consumer<TickerUpdate>> tickerListeners = new ConcurrentHashMap<>();

    // Binance WebSocket endpoint (NO API KEY NEEDED!)
    private static final String BINANCE_WS_URL = "wss://stream.binance.com:9443";

    /**
     * Ticker update from Binance
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class TickerUpdate {
        private String symbol; // e.g., "BTCUSDT"
        private BigDecimal price; // Current price
        private BigDecimal change24h; // 24h price change
        private BigDecimal changePercent24h; // 24h percent change
        private BigDecimal high24h; // 24h high
        private BigDecimal low24h; // 24h low
        private BigDecimal volume24h; // 24h volume
        private long eventTime; // Event timestamp
    }

    /**
     * Connect to Binance WebSocket streams
     */
    @PostConstruct
    public void connect() {
        // Connect with 2-second delay to ensure other beans are initialized
        scheduler.schedule(this::connectToStreams, 2, TimeUnit.SECONDS);
    }

    private void connectToStreams() {
        try {
            // Combined stream for BTC and ETH
            String streamUrl = BINANCE_WS_URL + "/stream?streams=btcusdt@ticker/ethusdt@ticker";

            WebSocketClient client = new StandardWebSocketClient();

            log.info("[Binance WebSocket] Connecting to: {}", streamUrl);

            WebSocketHandler handler = new WebSocketHandler() {
                @Override
                public void afterConnectionEstablished(WebSocketSession session) {
                    BinanceWebSocketClient.this.session = session;
                    log.info("[Binance WebSocket] ✅ Connected successfully!");
                    log.info("[Binance WebSocket] Receiving real-time ticker updates");
                }

                @Override
                public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) {
                    try {
                        String payload = message.getPayload().toString();
                        processTickerMessage(payload);
                    } catch (Exception e) {
                        log.error("[Binance WebSocket] Error processing message: {}", e.getMessage());
                    }
                }

                @Override
                public void handleTransportError(WebSocketSession session, Throwable exception) {
                    log.error("[Binance WebSocket] Transport error: {}", exception.getMessage());
                    scheduleReconnect();
                }

                @Override
                public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) {
                    log.warn("[Binance WebSocket] Connection closed: {}", closeStatus);
                    scheduleReconnect();
                }

                @Override
                public boolean supportsPartialMessages() {
                    return false;
                }
            };

            session = client.execute(handler, streamUrl).get(10, TimeUnit.SECONDS);

        } catch (Exception e) {
            log.error("[Binance WebSocket] Failed to connect: {}", e.getMessage());
            scheduleReconnect();
        }
    }

    /**
     * Process incoming ticker message from Binance
     */
    private void processTickerMessage(String payload) {
        try {
            JsonNode root = objectMapper.readTree(payload);

            // Combined stream format: {"stream":"btcusdt@ticker","data":{...}}
            String streamName = root.get("stream").asText();
            JsonNode data = root.get("data");

            // Extract symbol from stream name (e.g., "btcusdt@ticker" -> "BTCUSDT")
            String symbol = streamName.split("@")[0].toUpperCase();

            TickerUpdate update = TickerUpdate.builder()
                    .symbol(symbol)
                    .price(new BigDecimal(data.get("c").asText())) // Current price
                    .change24h(new BigDecimal(data.get("p").asText())) // 24h change
                    .changePercent24h(new BigDecimal(data.get("P").asText())) // Percent change
                    .high24h(new BigDecimal(data.get("h").asText()))
                    .low24h(new BigDecimal(data.get("l").asText()))
                    .volume24h(new BigDecimal(data.get("v").asText()))
                    .eventTime(data.get("E").asLong())
                    .build();

            log.debug("[Binance] {} = ${} ({}%)",
                    symbol, update.getPrice(), update.getChangePercent24h());

            // Notify listeners
            Consumer<TickerUpdate> listener = tickerListeners.get(symbol);
            if (listener != null) {
                listener.accept(update);
            }

        } catch (Exception e) {
            log.error("[Binance WebSocket] Failed to parse ticker: {}", e.getMessage());
        }
    }

    /**
     * Register a listener for ticker updates
     */
    public void registerTickerListener(String symbol, Consumer<TickerUpdate> listener) {
        tickerListeners.put(symbol.toUpperCase(), listener);
        log.info("[Binance WebSocket] Registered listener for {}", symbol);
    }

    /**
     * Schedule reconnection after disconnect
     */
    private void scheduleReconnect() {
        log.info("[Binance WebSocket] Reconnecting in 5 seconds...");
        scheduler.schedule(this::connectToStreams, 5, TimeUnit.SECONDS);
    }

    /**
     * Disconnect on shutdown
     */
    @PreDestroy
    public void disconnect() {
        log.info("[Binance WebSocket] Shutting down...");
        if (session != null && session.isOpen()) {
            try {
                session.close();
            } catch (IOException e) {
                log.error("[Binance WebSocket] Error closing session: {}", e.getMessage());
            }
        }
        scheduler.shutdown();
    }
}

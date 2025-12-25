package com.tradeflow.marketdata.websocket;

import com.tradeflow.common.constants.TradingPairs;
import com.tradeflow.marketdata.dto.TickerResponse;
import com.tradeflow.marketdata.service.MarketDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * WebSocket broadcaster for real-time market data updates.
 * Pushes ticker updates to subscribed clients.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MarketDataBroadcaster {

    private final SimpMessagingTemplate messagingTemplate;
    private final MarketDataService marketDataService;

    /**
     * Broadcast ticker updates to all subscribers
     */
    @Scheduled(fixedDelayString = "${tradeflow.market-data.websocket.broadcast-interval-ms:1000}")
    public void broadcastTickers() {
        for (String symbol : TradingPairs.SUPPORTED_PAIRS) {
            try {
                TickerResponse ticker = marketDataService.getTicker(symbol);
                if (ticker != null) {
                    // Broadcast to symbol-specific topic
                    messagingTemplate.convertAndSend("/topic/ticker/" + symbol.toLowerCase(), ticker);

                    // Also broadcast to general ticker topic
                    messagingTemplate.convertAndSend("/topic/tickers", ticker);
                }
            } catch (Exception e) {
                log.error("Error broadcasting ticker for {}: {}", symbol, e.getMessage());
            }
        }
    }

    /**
     * Broadcast a specific ticker update (called after trade execution)
     */
    public void broadcastTickerUpdate(TickerResponse ticker) {
        if (ticker != null && ticker.getSymbol() != null) {
            messagingTemplate.convertAndSend("/topic/ticker/" + ticker.getSymbol().toLowerCase(), ticker);
            messagingTemplate.convertAndSend("/topic/tickers", ticker);
            log.debug("Broadcast ticker update for {}", ticker.getSymbol());
        }
    }
}

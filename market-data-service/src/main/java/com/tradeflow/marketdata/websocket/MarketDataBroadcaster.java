package com.tradeflow.marketdata.websocket;

import com.tradeflow.marketdata.dto.TickerResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

/**
 * WebSocket broadcaster for real-time market data updates.
 * Pushes ticker updates to subscribed clients.
 * 
 * NOW PUSH-BASED (via Binance WebSocket) instead of polling!
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MarketDataBroadcaster {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * NOTE: Removed scheduled broadcast - now using real-time Binance WebSocket
     * push!
     * MarketDataService calls broadcastTickerUpdate() directly when new data
     * arrives.
     */

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

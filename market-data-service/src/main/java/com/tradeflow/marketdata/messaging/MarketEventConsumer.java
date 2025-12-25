package com.tradeflow.marketdata.messaging;

import com.tradeflow.common.constants.KafkaTopics;
import com.tradeflow.common.event.OrderBookUpdateEvent;
import com.tradeflow.common.event.TradeExecutedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

/**
 * Kafka consumer for trade and order book events from Matching Engine.
 * Broadcasts updates to WebSocket clients.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MarketEventConsumer {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Handle trade executed events
     */
    @KafkaListener(topics = KafkaTopics.TRADES_EXECUTED, groupId = "market-data-service")
    public void handleTradeExecuted(TradeExecutedEvent event) {
        log.info("Trade executed: {} {} @ {}",
                event.getSymbol(), event.getQuantity(), event.getPrice());

        // Broadcast to trade topic for the symbol
        messagingTemplate.convertAndSend(
                "/topic/trades/" + event.getSymbol().toLowerCase(),
                event);

        // Broadcast to general trades topic
        messagingTemplate.convertAndSend("/topic/trades", event);
    }

    /**
     * Handle order book update events
     */
    @KafkaListener(topics = KafkaTopics.ORDERBOOK_UPDATES, groupId = "market-data-service")
    public void handleOrderBookUpdate(OrderBookUpdateEvent event) {
        log.debug("Order book update for {}", event.getSymbol());

        // Broadcast to order book topic for the symbol
        messagingTemplate.convertAndSend(
                "/topic/orderbook/" + event.getSymbol().toLowerCase(),
                event);
    }
}

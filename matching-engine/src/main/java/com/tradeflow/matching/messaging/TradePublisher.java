package com.tradeflow.matching.messaging;

import com.tradeflow.common.constants.KafkaTopics;
import com.tradeflow.common.event.OrderBookUpdateEvent;
import com.tradeflow.common.event.TradeExecutedEvent;
import com.tradeflow.matching.engine.MatchingEngine;
import com.tradeflow.matching.orderbook.OrderBook;
import com.tradeflow.matching.orderbook.Trade;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Publishes trade events and order book updates to Kafka
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TradePublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final MatchingEngine matchingEngine;

    /**
     * Publish trade execution events
     */
    public void publishTrades(List<Trade> trades) {
        for (Trade trade : trades) {
            TradeExecutedEvent event = TradeExecutedEvent.builder()
                    .tradeId(trade.getTradeId())
                    .symbol(trade.getSymbol())
                    .buyOrderId(trade.getBuyOrderId())
                    .buyUserId(trade.getBuyUserId())
                    .sellOrderId(trade.getSellOrderId())
                    .sellUserId(trade.getSellUserId())
                    .price(trade.getPrice())
                    .quantity(trade.getQuantity())
                    .makerOrderId(trade.getMakerOrderId())
                    .takerOrderId(trade.getTakerOrderId())
                    .timestamp(trade.getTimestamp())
                    .build();

            kafkaTemplate.send(KafkaTopics.TRADES_EXECUTED, trade.getSymbol(), event);
            log.debug("Published trade event: {} {} @ {}",
                    trade.getTradeId(), trade.getQuantity(), trade.getPrice());
        }
    }

    /**
     * Publish order book update event
     */
    public void publishOrderBookUpdate(String symbol) {
        OrderBook.OrderBookSnapshot snapshot = matchingEngine.getSnapshot(symbol, 10);
        if (snapshot == null) {
            return;
        }

        // Convert to DTO format
        List<OrderBookUpdateEvent.PriceLevel> bids = snapshot.bids().stream()
                .map(pl -> new OrderBookUpdateEvent.PriceLevel(pl.price(), pl.quantity()))
                .collect(Collectors.toList());

        List<OrderBookUpdateEvent.PriceLevel> asks = snapshot.asks().stream()
                .map(pl -> new OrderBookUpdateEvent.PriceLevel(pl.price(), pl.quantity()))
                .collect(Collectors.toList());

        OrderBookUpdateEvent event = OrderBookUpdateEvent.builder()
                .symbol(symbol)
                .bids(bids)
                .asks(asks)
                .timestamp(Instant.now())
                .build();

        kafkaTemplate.send(KafkaTopics.ORDERBOOK_UPDATES, symbol, event);
        log.debug("Published order book update for {}", symbol);
    }
}

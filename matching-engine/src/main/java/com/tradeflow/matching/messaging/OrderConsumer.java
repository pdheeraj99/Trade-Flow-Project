package com.tradeflow.matching.messaging;

import com.tradeflow.common.event.OrderToMatchingEvent;
import com.tradeflow.common.enums.OrderSide;
import com.tradeflow.common.enums.OrderType;
import com.tradeflow.matching.engine.MatchingEngine;
import com.tradeflow.matching.orderbook.BookOrder;
import com.tradeflow.matching.orderbook.MatchResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Kafka consumer for incoming orders from OMS
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderConsumer {

    private final MatchingEngine matchingEngine;
    private final TradePublisher tradePublisher;

    /**
     * Process incoming orders from OMS via Kafka
     */
    @KafkaListener(topics = "orders.incoming", groupId = "matching-engine", concurrency = "10")
    public void handleOrder(OrderToMatchingEvent event, Acknowledgment ack) {
        try {
            log.info("Received order from OMS: {}", event.getOrderId());

            BookOrder order = parseOrder(event);

            // Process order through matching engine
            MatchResult result = matchingEngine.processOrder(order);

            // Publish trades if any
            if (result.hasTrades()) {
                tradePublisher.publishTrades(result.getTrades());
                log.info("Order {} produced {} trades", order.getOrderId(), result.getTrades().size());
            }

            // Publish order book update
            tradePublisher.publishOrderBookUpdate(order.getSymbol());

            // Acknowledge message processing
            ack.acknowledge();
            log.debug("Order {} processed and acknowledged", order.getOrderId());

        } catch (Exception e) {
            log.error("Error processing order: {}", e.getMessage(), e);
            // In Kafka, we might want to dead-letter this or just log it.
            // Acking it prevents infinite poison-pill loops in this simple implementation.
            ack.acknowledge();
        }
    }

    /**
     * Parse BookOrder from event
     */
    private BookOrder parseOrder(OrderToMatchingEvent event) {
        return BookOrder.builder()
                .orderId(event.getOrderId())
                .userId(event.getUserId())
                .symbol(event.getSymbol())
                .side(OrderSide.valueOf(event.getSide().name()))
                .type(OrderType.valueOf(event.getType().name()))
                .price(event.getPrice())
                .originalQuantity(event.getQuantity())
                .remainingQuantity(event.getQuantity())
                .timestamp(event.getTimestamp() != null ? event.getTimestamp() : Instant.now())
                .sequenceNumber(0)
                .build();
    }
}

package com.tradeflow.matching.messaging;

import com.tradeflow.common.constants.KafkaTopics;
import com.tradeflow.common.enums.OrderSide;
import com.tradeflow.common.enums.OrderType;
import com.tradeflow.matching.engine.MatchingEngine;
import com.tradeflow.matching.orderbook.BookOrder;
import com.tradeflow.matching.orderbook.MatchResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

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
     * Process incoming orders from OMS
     */
    @KafkaListener(topics = KafkaTopics.ORDERS_TO_MATCHING, groupId = "matching-engine")
    public void handleOrder(ConsumerRecord<String, Map<String, Object>> record, Acknowledgment ack) {
        try {
            Map<String, Object> payload = record.value();
            log.info("Received order from OMS: {}", payload.get("orderId"));

            // Parse order from payload
            BookOrder order = parseOrder(payload);

            // Process order through matching engine
            MatchResult result = matchingEngine.processOrder(order);

            // Publish trades if any
            if (result.hasTrades()) {
                tradePublisher.publishTrades(result.getTrades());
                log.info("Order {} produced {} trades", order.getOrderId(), result.getTrades().size());
            }

            // Publish order book update
            tradePublisher.publishOrderBookUpdate(order.getSymbol());

            ack.acknowledge();
            log.debug("Order {} processed and acknowledged", order.getOrderId());

        } catch (Exception e) {
            log.error("Error processing order: {}", e.getMessage(), e);
            // In production, implement dead-letter logic
            ack.acknowledge(); // Still ack to avoid reprocessing bad message
        }
    }

    /**
     * Parse BookOrder from Kafka payload
     */
    private BookOrder parseOrder(Map<String, Object> payload) {
        return BookOrder.builder()
                .orderId(UUID.fromString((String) payload.get("orderId")))
                .userId(UUID.fromString((String) payload.get("userId")))
                .symbol((String) payload.get("symbol"))
                .side(OrderSide.valueOf((String) payload.get("side")))
                .type(OrderType.valueOf((String) payload.get("type")))
                .price(payload.get("price") != null ? new BigDecimal(payload.get("price").toString()) : null)
                .originalQuantity(new BigDecimal(payload.get("quantity").toString()))
                .remainingQuantity(new BigDecimal(payload.get("quantity").toString()))
                .timestamp(Instant.now())
                .sequenceNumber(0)
                .build();
    }
}

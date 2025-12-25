package com.tradeflow.oms.messaging;

import com.tradeflow.common.constants.KafkaTopics;
import com.tradeflow.common.event.TradeExecutedEvent;
import com.tradeflow.oms.saga.OrderSagaOrchestrator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Kafka consumer for trade execution events from Matching Engine
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TradeEventHandler {

    private final OrderSagaOrchestrator sagaOrchestrator;

    /**
     * Handle trade executed event from matching engine
     */
    @KafkaListener(topics = KafkaTopics.TRADES_EXECUTED, groupId = "oms-service")
    public void handleTradeExecuted(TradeExecutedEvent event) {
        log.info("Received TradeExecutedEvent: trade {} - {} @ {}",
                event.getTradeId(), event.getQuantity(), event.getPrice());

        try {
            // Update buy order
            sagaOrchestrator.onTradeExecuted(
                    event.getBuyOrderId(),
                    event.getQuantity(),
                    event.getPrice());

            // Update sell order
            sagaOrchestrator.onTradeExecuted(
                    event.getSellOrderId(),
                    event.getQuantity(),
                    event.getPrice());

            log.info("Processed trade {} for buy order {} and sell order {}",
                    event.getTradeId(), event.getBuyOrderId(), event.getSellOrderId());

        } catch (Exception e) {
            log.error("Error processing trade event {}", event.getTradeId(), e);
            // In production, implement retry/dead-letter logic
            throw e;
        }
    }
}

package com.tradeflow.matching.messaging;

import com.rabbitmq.client.Channel;
import com.tradeflow.common.event.OrderToMatchingEvent;
import com.tradeflow.common.enums.OrderSide;
import com.tradeflow.common.enums.OrderType;
import com.tradeflow.matching.engine.MatchingEngine;
import com.tradeflow.matching.orderbook.BookOrder;
import com.tradeflow.matching.orderbook.MatchResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;

/**
 * RabbitMQ consumer for incoming orders from OMS
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
    @RabbitListener(queues = "#{@matchingOrderQueue.name}")
    public void handleOrder(OrderToMatchingEvent event, Message message, Channel channel) throws IOException {
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

            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
            log.debug("Order {} processed and acknowledged", order.getOrderId());

        } catch (Exception e) {
            log.error("Error processing order: {}", e.getMessage(), e);
            
            // Send to DLQ after max retries
            if (shouldSendToDLQ(message)) {
                log.warn("Sending order {} to DLQ after processing failures", event.getOrderId());
                channel.basicReject(message.getMessageProperties().getDeliveryTag(), false);
            } else {
                // Retry by requeuing with exponential backoff handled by RabbitMQ
                log.info("Requeuing order {} for retry", event.getOrderId());
                channel.basicNack(message.getMessageProperties().getDeliveryTag(), false, true);
            }
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

    /**
     * Determine if message should be sent to DLQ based on retry count
     */
    private boolean shouldSendToDLQ(Message message) {
        // Check x-death header to count previous attempts
        if (message.getMessageProperties().getHeaders() != null) {
            Object deathHeader = message.getMessageProperties().getHeaders().get("x-death");
            if (deathHeader instanceof java.util.List) {
                java.util.List<?> deathList = (java.util.List<?>) deathHeader;
                if (!deathList.isEmpty() && deathList.get(0) instanceof java.util.Map) {
                    Object count = ((java.util.Map<?, ?>) deathList.get(0)).get("count");
                    if (count instanceof Number) {
                        int retryCount = ((Number) count).intValue();
                        // Send to DLQ after 3 retry attempts
                        return retryCount >= 3;
                    }
                }
            }
        }
        return false; // First attempt, allow retry
    }
}

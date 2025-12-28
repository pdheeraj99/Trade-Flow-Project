package com.tradeflow.oms.messaging;

import com.tradeflow.common.command.SettleTradeCommand;
import com.tradeflow.common.constants.KafkaTopics;
import com.tradeflow.common.constants.RabbitMQConstants;
import com.tradeflow.common.event.TradeExecutedEvent;
import com.tradeflow.oms.saga.OrderSagaOrchestrator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.UUID;

/**
 * Kafka consumer for trade execution events from Matching Engine
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TradeEventHandler {

    private final OrderSagaOrchestrator sagaOrchestrator;
    private final RabbitTemplate rabbitTemplate;

    private static final int SCALE = 8;

    /**
     * Handle trade executed event from matching engine
     */
    @KafkaListener(topics = KafkaTopics.TRADES_EXECUTED, groupId = "oms-service")
    public void handleTradeExecuted(TradeExecutedEvent event) {
        log.info("Received TradeExecutedEvent: trade {} - {} @ {}",
                event.getTradeId(), event.getQuantity(), event.getPrice());

        try {
            publishSettlementCommand(event);

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

    private void publishSettlementCommand(TradeExecutedEvent event) {
        String symbol = event.getSymbol().toUpperCase();
        String baseCurrency = symbol.replace("USDT", "").replace("USD", "");
        String quoteCurrency = symbol.endsWith("USDT") ? "USDT" : "USD";

        BigDecimal baseAmount = event.getQuantity().setScale(SCALE, RoundingMode.HALF_UP);
        BigDecimal quoteAmount = event.getPrice()
                .multiply(event.getQuantity())
                .setScale(SCALE, RoundingMode.HALF_UP);

        SettleTradeCommand command = SettleTradeCommand.builder()
                .commandId(UUID.randomUUID())
                .sagaId(event.getTradeId())
                .tradeId(event.getTradeId())
                .symbol(symbol)
                .buyerId(event.getBuyUserId())
                .buyerDebitCurrency(quoteCurrency)
                .buyerDebitAmount(quoteAmount.toPlainString())
                .buyerCreditCurrency(baseCurrency)
                .buyerCreditAmount(baseAmount.toPlainString())
                .sellerId(event.getSellUserId())
                .sellerDebitCurrency(baseCurrency)
                .sellerDebitAmount(baseAmount.toPlainString())
                .sellerCreditCurrency(quoteCurrency)
                .sellerCreditAmount(quoteAmount.toPlainString())
                .commandTimestamp(Instant.now())
                .build();

        rabbitTemplate.convertAndSend(
                RabbitMQConstants.WALLET_EXCHANGE,
                RabbitMQConstants.ROUTING_WALLET_SETTLE,
                command);

        log.info("Published SettleTradeCommand for trade {}", event.getTradeId());
    }
}

package com.tradeflow.wallet.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import com.tradeflow.common.command.ReleaseFundsCommand;
import com.tradeflow.common.command.ReserveFundsCommand;
import com.tradeflow.common.command.SettleTradeCommand;
import com.tradeflow.common.constants.RabbitMQConstants;
import com.tradeflow.common.event.FundsReservationFailedEvent;
import com.tradeflow.common.event.FundsReservedEvent;
import com.tradeflow.wallet.exception.InsufficientFundsException;
import com.tradeflow.wallet.exception.WalletNotFoundException;
import com.tradeflow.wallet.service.WalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * RabbitMQ message handlers for Saga commands.
 * Implements idempotency using Redis to prevent duplicate processing.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class WalletCommandHandler {

    private final WalletService walletService;
    private final RabbitTemplate rabbitTemplate;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String PROCESSED_KEY_PREFIX = "processed:saga:";
    private static final Duration IDEMPOTENCY_TTL = Duration.ofDays(1);

    /**
     * Handle Reserve Funds Command from OMS
     */
    @RabbitListener(queues = RabbitMQConstants.WALLET_RESERVE_QUEUE)
    public void handleReserveFunds(ReserveFundsCommand command, Message message, Channel channel) throws IOException {
        String sagaId = command.getSagaId().toString();
        log.info("Received ReserveFundsCommand for saga {}", sagaId);

        try {
            // Idempotency check
            if (isAlreadyProcessed(sagaId)) {
                log.warn("Saga {} already processed, acknowledging and skipping", sagaId);
                channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
                return;
            }

            // Process the command
            walletService.reserveFunds(
                    command.getUserId(),
                    command.getCurrency(),
                    new BigDecimal(command.getAmount()),
                    command.getOrderId());

            // Mark as processed
            markAsProcessed(sagaId);

            // Send success event
            FundsReservedEvent event = FundsReservedEvent.builder()
                    .eventId(UUID.randomUUID())
                    .sagaId(command.getSagaId())
                    .orderId(command.getOrderId())
                    .userId(command.getUserId())
                    .currency(command.getCurrency())
                    .amount(command.getAmount())
                    .eventTimestamp(Instant.now())
                    .build();

            rabbitTemplate.convertAndSend(
                    RabbitMQConstants.ORDER_EXCHANGE,
                    RabbitMQConstants.ROUTING_ORDER_RESPONSE,
                    event);

            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
            log.info("Successfully reserved funds for saga {}", sagaId);

        } catch (InsufficientFundsException e) {
            log.warn("Insufficient funds for saga {}: {}", sagaId, e.getMessage());

            // Send failure event
            FundsReservationFailedEvent event = FundsReservationFailedEvent.builder()
                    .eventId(UUID.randomUUID())
                    .sagaId(command.getSagaId())
                    .orderId(command.getOrderId())
                    .userId(command.getUserId())
                    .currency(command.getCurrency())
                    .requestedAmount(command.getAmount())
                    .availableBalance(e.getAvailable().toString())
                    .reason("INSUFFICIENT_FUNDS")
                    .eventTimestamp(Instant.now())
                    .build();

            rabbitTemplate.convertAndSend(
                    RabbitMQConstants.ORDER_EXCHANGE,
                    RabbitMQConstants.ROUTING_ORDER_RESPONSE,
                    event);

            // Mark as processed (even failure)
            markAsProcessed(sagaId);
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);

        } catch (WalletNotFoundException e) {
            log.error("Wallet not found for saga {}: {}", sagaId, e.getMessage());

            FundsReservationFailedEvent event = FundsReservationFailedEvent.builder()
                    .eventId(UUID.randomUUID())
                    .sagaId(command.getSagaId())
                    .orderId(command.getOrderId())
                    .userId(command.getUserId())
                    .currency(command.getCurrency())
                    .requestedAmount(command.getAmount())
                    .reason("WALLET_NOT_FOUND")
                    .eventTimestamp(Instant.now())
                    .build();

            rabbitTemplate.convertAndSend(
                    RabbitMQConstants.ORDER_EXCHANGE,
                    RabbitMQConstants.ROUTING_ORDER_RESPONSE,
                    event);

            markAsProcessed(sagaId);
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);

        } catch (Exception e) {
            log.error("Error processing ReserveFundsCommand for saga {}", sagaId, e);
            // Reject and requeue for retry
            channel.basicNack(message.getMessageProperties().getDeliveryTag(), false, true);
        }
    }

    /**
     * Handle Release Funds Command (Saga compensation)
     */
    @RabbitListener(queues = RabbitMQConstants.WALLET_RELEASE_QUEUE)
    public void handleReleaseFunds(ReleaseFundsCommand command, Message message, Channel channel) throws IOException {
        String sagaId = command.getSagaId().toString() + ":release";
        log.info("Received ReleaseFundsCommand for saga {}", command.getSagaId());

        try {
            if (isAlreadyProcessed(sagaId)) {
                log.warn("Release for saga {} already processed, skipping", command.getSagaId());
                channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
                return;
            }

            walletService.releaseFunds(
                    command.getUserId(),
                    command.getCurrency(),
                    new BigDecimal(command.getAmount()),
                    command.getOrderId());

            markAsProcessed(sagaId);
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
            log.info("Successfully released funds for saga {}", command.getSagaId());

        } catch (Exception e) {
            log.error("Error processing ReleaseFundsCommand for saga {}", command.getSagaId(), e);
            channel.basicNack(message.getMessageProperties().getDeliveryTag(), false, true);
        }
    }

    /**
     * Handle Settle Trade Command
     */
    @RabbitListener(queues = RabbitMQConstants.WALLET_SETTLE_QUEUE)
    public void handleSettleTrade(SettleTradeCommand command, Message message, Channel channel) throws IOException {
        String sagaId = command.getSagaId().toString() + ":settle";
        log.info("Received SettleTradeCommand for trade {}", command.getTradeId());

        try {
            if (isAlreadyProcessed(sagaId)) {
                log.warn("Settlement for trade {} already processed, skipping", command.getTradeId());
                channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
                return;
            }

            walletService.settleTrade(
                    command.getBuyerId(),
                    command.getSellerId(),
                    command.getBuyerCreditCurrency(), // Base currency (e.g., BTC)
                    command.getBuyerDebitCurrency(), // Quote currency (e.g., USDT)
                    new BigDecimal(command.getBuyerCreditAmount()),
                    new BigDecimal(command.getBuyerDebitAmount()),
                    command.getTradeId());

            markAsProcessed(sagaId);
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
            log.info("Successfully settled trade {}", command.getTradeId());

        } catch (Exception e) {
            log.error("Error processing SettleTradeCommand for trade {}", command.getTradeId(), e);
            channel.basicNack(message.getMessageProperties().getDeliveryTag(), false, true);
        }
    }

    /**
     * Check if saga has already been processed (idempotency)
     */
    private boolean isAlreadyProcessed(String key) {
        String redisKey = PROCESSED_KEY_PREFIX + key;
        return Boolean.TRUE.equals(redisTemplate.hasKey(redisKey));
    }

    /**
     * Mark saga as processed
     */
    private void markAsProcessed(String key) {
        String redisKey = PROCESSED_KEY_PREFIX + key;
        redisTemplate.opsForValue().set(redisKey, "1", IDEMPOTENCY_TTL);
    }
}

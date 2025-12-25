package com.tradeflow.oms.saga;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradeflow.common.command.ReleaseFundsCommand;
import com.tradeflow.common.command.ReserveFundsCommand;
import com.tradeflow.common.constants.KafkaTopics;
import com.tradeflow.common.constants.RabbitMQConstants;
import com.tradeflow.common.enums.OrderSide;
import com.tradeflow.common.enums.OrderStatus;
import com.tradeflow.common.enums.OrderType;
import com.tradeflow.common.enums.SagaState;
import com.tradeflow.oms.entity.Order;
import com.tradeflow.oms.entity.SagaInstance;
import com.tradeflow.oms.repository.OrderRepository;
import com.tradeflow.oms.repository.SagaInstanceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.UUID;

/**
 * Saga Orchestrator for order processing.
 * Coordinates the distributed transaction flow:
 * 1. Create Order → 2. Reserve Funds → 3. Send to Matching Engine
 * 
 * Compensation flow on failure:
 * Release Funds → Mark Order as Rejected/Cancelled
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrderSagaOrchestrator {

    private final OrderRepository orderRepository;
    private final SagaInstanceRepository sagaRepository;
    private final RabbitTemplate rabbitTemplate;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    private static final int SCALE = 8;

    /**
     * Start a new order saga
     */
    @Transactional
    public SagaInstance startOrderSaga(Order order) {
        log.info("Starting saga for order {}", order.getOrderId());

        // Build saga context
        OrderSagaContext context = buildContext(order);

        // Create saga instance
        SagaInstance saga = SagaInstance.builder()
                .order(order)
                .state(SagaState.STARTED)
                .currentStep("INIT")
                .payload(serializeContext(context))
                .build();
        saga = sagaRepository.save(saga);

        // Update order status
        order.setStatus(OrderStatus.PENDING_VALIDATION);
        order.setReservedAmount(context.getReserveAmount());
        orderRepository.save(order);

        log.info("Saga {} created for order {}", saga.getSagaId(), order.getOrderId());

        // Initiate fund reservation
        requestFundReservation(saga, context);

        return saga;
    }

    /**
     * Step 1: Request fund reservation from Wallet Service
     */
    private void requestFundReservation(SagaInstance saga, OrderSagaContext context) {
        log.info("Saga {}: Requesting fund reservation", saga.getSagaId());

        ReserveFundsCommand command = ReserveFundsCommand.builder()
                .commandId(UUID.randomUUID())
                .sagaId(saga.getSagaId())
                .orderId(context.getOrderId())
                .userId(context.getUserId())
                .currency(context.getReserveCurrency())
                .amount(context.getReserveAmount().toPlainString())
                .reason(String.format("%s_ORDER:%s", context.getSide(), context.getSymbol()))
                .commandTimestamp(Instant.now())
                .build();

        rabbitTemplate.convertAndSend(
                RabbitMQConstants.WALLET_EXCHANGE,
                RabbitMQConstants.ROUTING_WALLET_RESERVE,
                command);

        // Update saga state
        saga.transitionTo(SagaState.AWAITING_FUNDS);
        saga.setCurrentStep("RESERVE_FUNDS");
        sagaRepository.save(saga);

        log.info("Saga {}: Reserve funds command sent", saga.getSagaId());
    }

    /**
     * Handle successful fund reservation (called by response handler)
     */
    @Transactional
    public void onFundsReserved(UUID sagaId, String transactionId) {
        log.info("Saga {}: Funds reserved successfully", sagaId);

        SagaInstance saga = sagaRepository.findById(sagaId)
                .orElseThrow(() -> new RuntimeException("Saga not found: " + sagaId));

        OrderSagaContext context = deserializeContext(saga.getPayload());
        context.setFundsReserved(true);
        context.setWalletTransactionId(transactionId);

        // Update saga
        saga.transitionTo(SagaState.FUNDS_RESERVED);
        saga.setCurrentStep("FUNDS_RESERVED");
        saga.setPayload(serializeContext(context));
        sagaRepository.save(saga);

        // Update order status
        Order order = saga.getOrder();
        order.setStatus(OrderStatus.FUNDS_RESERVED);
        orderRepository.save(order);

        // Send order to matching engine
        sendToMatchingEngine(saga, context);
    }

    /**
     * Step 2: Send order to Matching Engine via Kafka
     */
    private void sendToMatchingEngine(SagaInstance saga, OrderSagaContext context) {
        log.info("Saga {}: Sending order to matching engine", saga.getSagaId());

        Order order = saga.getOrder();

        // Build order event for matching engine
        OrderToMatchingEvent event = OrderToMatchingEvent.builder()
                .orderId(order.getOrderId())
                .userId(order.getUserId())
                .symbol(order.getSymbol())
                .side(order.getSide())
                .type(order.getType())
                .price(order.getPrice())
                .quantity(order.getQuantity())
                .timestamp(Instant.now())
                .build();

        // Send to Kafka, partitioned by symbol
        kafkaTemplate.send(KafkaTopics.ORDERS_TO_MATCHING, order.getSymbol(), event);

        // Update saga and order
        saga.transitionTo(SagaState.ORDER_SENT);
        saga.setCurrentStep("ORDER_SENT");
        context.setOrderSentToMatching(true);
        saga.setPayload(serializeContext(context));
        sagaRepository.save(saga);

        order.setStatus(OrderStatus.OPEN);
        orderRepository.save(order);

        log.info("Saga {}: Order sent to matching engine", saga.getSagaId());
    }

    /**
     * Handle fund reservation failure
     */
    @Transactional
    public void onFundsReservationFailed(UUID sagaId, String reason) {
        log.warn("Saga {}: Fund reservation failed - {}", sagaId, reason);

        SagaInstance saga = sagaRepository.findById(sagaId)
                .orElseThrow(() -> new RuntimeException("Saga not found: " + sagaId));

        // Update saga
        saga.fail("Fund reservation failed: " + reason);
        sagaRepository.save(saga);

        // Update order
        Order order = saga.getOrder();
        order.setStatus(OrderStatus.REJECTED);
        order.setRejectReason(reason);
        orderRepository.save(order);

        log.info("Saga {}: Order rejected due to insufficient funds", sagaId);
    }

    /**
     * Handle trade execution (called when matching engine fills order)
     */
    @Transactional
    public void onTradeExecuted(UUID orderId, BigDecimal filledQuantity, BigDecimal fillPrice) {
        log.info("Order {}: Trade executed - {} @ {}", orderId, filledQuantity, fillPrice);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));

        // Update fill information
        BigDecimal newFilledQty = order.getFilledQuantity().add(filledQuantity);
        order.setFilledQuantity(newFilledQty);

        // Update average fill price
        if (order.getAvgFillPrice() == null) {
            order.setAvgFillPrice(fillPrice);
        } else {
            // Weighted average
            BigDecimal oldValue = order.getAvgFillPrice().multiply(order.getFilledQuantity().subtract(filledQuantity));
            BigDecimal newValue = fillPrice.multiply(filledQuantity);
            order.setAvgFillPrice(oldValue.add(newValue).divide(newFilledQty, SCALE, RoundingMode.HALF_UP));
        }

        // Update status
        if (order.isFullyFilled()) {
            order.setStatus(OrderStatus.FILLED);
            order.setFilledAt(Instant.now());

            // Complete the saga
            SagaInstance saga = sagaRepository.findByOrderOrderId(orderId).orElse(null);
            if (saga != null) {
                saga.complete();
                sagaRepository.save(saga);
            }
        } else {
            order.setStatus(OrderStatus.PARTIALLY_FILLED);
        }

        orderRepository.save(order);
        log.info("Order {} updated: filled {}/{}", orderId, order.getFilledQuantity(), order.getQuantity());
    }

    /**
     * Cancel an order (triggers compensation)
     */
    @Transactional
    public void cancelOrder(UUID orderId, String reason) {
        log.info("Cancelling order {}: {}", orderId, reason);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));

        if (!order.isCancellable()) {
            throw new IllegalStateException("Order cannot be cancelled in state: " + order.getStatus());
        }

        SagaInstance saga = sagaRepository.findByOrderOrderId(orderId).orElse(null);
        if (saga != null) {
            compensate(saga, reason);
        }

        // Update order
        order.setStatus(OrderStatus.CANCELLED);
        order.setRejectReason(reason);
        orderRepository.save(order);

        log.info("Order {} cancelled", orderId);
    }

    /**
     * Compensation: Release reserved funds
     */
    private void compensate(SagaInstance saga, String reason) {
        log.info("Saga {}: Starting compensation", saga.getSagaId());

        OrderSagaContext context = deserializeContext(saga.getPayload());

        // Only release funds if they were reserved
        if (context.isFundsReserved()) {
            ReleaseFundsCommand command = ReleaseFundsCommand.builder()
                    .commandId(UUID.randomUUID())
                    .sagaId(saga.getSagaId())
                    .orderId(context.getOrderId())
                    .userId(context.getUserId())
                    .currency(context.getReserveCurrency())
                    .amount(context.getReserveAmount().toPlainString())
                    .reason(reason)
                    .commandTimestamp(Instant.now())
                    .build();

            rabbitTemplate.convertAndSend(
                    RabbitMQConstants.WALLET_EXCHANGE,
                    RabbitMQConstants.ROUTING_WALLET_RELEASE,
                    command);
        }

        saga.transitionTo(SagaState.COMPENSATING);
        saga.setCurrentStep("RELEASE_FUNDS");
        sagaRepository.save(saga);
    }

    /**
     * Build saga context from order
     */
    private OrderSagaContext buildContext(Order order) {
        // Parse symbol (e.g., BTCUSDT → BTC, USDT)
        String symbol = order.getSymbol().toUpperCase();
        String baseCurrency = symbol.replace("USDT", "").replace("USD", "");
        String quoteCurrency = symbol.endsWith("USDT") ? "USDT" : "USD";

        // Calculate reserve amount
        BigDecimal reserveAmount;
        String reserveCurrency;

        if (order.getSide() == OrderSide.BUY) {
            // Buy order: reserve quote currency (e.g., USDT)
            reserveCurrency = quoteCurrency;
            if (order.getType() == OrderType.LIMIT) {
                // For limit buy: reserve price * quantity
                reserveAmount = order.getPrice().multiply(order.getQuantity())
                        .setScale(SCALE, RoundingMode.HALF_UP);
            } else {
                // For market buy: need to estimate (use a buffer - this is simplified)
                // In production, you'd get the best ask price * quantity * buffer
                reserveAmount = order.getQuantity().multiply(new BigDecimal("100000"))
                        .setScale(SCALE, RoundingMode.HALF_UP);
            }
        } else {
            // Sell order: reserve base currency (e.g., BTC)
            reserveCurrency = baseCurrency;
            reserveAmount = order.getQuantity();
        }

        return OrderSagaContext.builder()
                .orderId(order.getOrderId())
                .userId(order.getUserId())
                .symbol(symbol)
                .side(order.getSide())
                .type(order.getType())
                .price(order.getPrice())
                .quantity(order.getQuantity())
                .baseCurrency(baseCurrency)
                .quoteCurrency(quoteCurrency)
                .reserveAmount(reserveAmount)
                .reserveCurrency(reserveCurrency)
                .fundsReserved(false)
                .orderSentToMatching(false)
                .build();
    }

    private String serializeContext(OrderSagaContext context) {
        try {
            return objectMapper.writeValueAsString(context);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize saga context", e);
        }
    }

    private OrderSagaContext deserializeContext(String payload) {
        try {
            return objectMapper.readValue(payload, OrderSagaContext.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to deserialize saga context", e);
        }
    }
}

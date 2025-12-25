package com.tradeflow.oms.service;

import com.tradeflow.common.enums.OrderStatus;
import com.tradeflow.common.enums.OrderType;
import com.tradeflow.oms.dto.CancelOrderRequest;
import com.tradeflow.oms.dto.OrderResponse;
import com.tradeflow.oms.dto.PlaceOrderRequest;
import com.tradeflow.oms.entity.Order;
import com.tradeflow.oms.exception.DuplicateOrderException;
import com.tradeflow.oms.exception.OrderNotFoundException;
import com.tradeflow.oms.repository.OrderRepository;
import com.tradeflow.oms.saga.OrderSagaOrchestrator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Order Service handling order placement and management.
 * Delegates to Saga Orchestrator for distributed transaction coordination.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderSagaOrchestrator sagaOrchestrator;

    /**
     * Place a new order
     */
    @Transactional
    public OrderResponse placeOrder(UUID userId, PlaceOrderRequest request) {
        log.info("Placing order for user {}: {} {} {} @ {}",
                userId, request.getSide(), request.getQuantity(),
                request.getSymbol(), request.getPrice());

        // Check for duplicate client order ID
        if (request.getClientOrderId() != null && !request.getClientOrderId().isEmpty()) {
            if (orderRepository.existsByClientOrderId(request.getClientOrderId())) {
                throw new DuplicateOrderException(request.getClientOrderId());
            }
        }

        // Validate limit order has price
        if (request.getType() == OrderType.LIMIT &&
                (request.getPrice() == null || request.getPrice().compareTo(BigDecimal.ZERO) <= 0)) {
            throw new IllegalArgumentException("Limit orders require a positive price");
        }

        // Create order entity
        Order order = Order.builder()
                .userId(userId)
                .symbol(request.getSymbol().toUpperCase())
                .side(request.getSide())
                .type(request.getType())
                .price(request.getPrice())
                .quantity(request.getQuantity())
                .filledQuantity(BigDecimal.ZERO)
                .status(OrderStatus.PENDING_VALIDATION)
                .clientOrderId(request.getClientOrderId())
                .build();
        order = orderRepository.save(order);

        log.info("Order {} created for user {}", order.getOrderId(), userId);

        // Start the saga
        sagaOrchestrator.startOrderSaga(order);

        return toResponse(order);
    }

    /**
     * Cancel an order
     */
    @Transactional
    public OrderResponse cancelOrder(UUID userId, CancelOrderRequest request) {
        log.info("User {} cancelling order {}", userId, request.getOrderId());

        Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new OrderNotFoundException(request.getOrderId()));

        // Verify ownership
        if (!order.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Order does not belong to user");
        }

        if (!order.isCancellable()) {
            throw new IllegalStateException("Order cannot be cancelled in state: " + order.getStatus());
        }

        // Trigger cancellation through saga
        String reason = request.getReason() != null ? request.getReason() : "User requested cancellation";
        sagaOrchestrator.cancelOrder(request.getOrderId(), reason);

        // Refresh order state
        order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new OrderNotFoundException(request.getOrderId()));

        return toResponse(order);
    }

    /**
     * Get order by ID
     */
    @Transactional(readOnly = true)
    public OrderResponse getOrder(UUID userId, UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));

        // Verify ownership
        if (!order.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Order does not belong to user");
        }

        return toResponse(order);
    }

    /**
     * Get user's orders with pagination
     */
    @Transactional(readOnly = true)
    public Page<OrderResponse> getUserOrders(UUID userId, Pageable pageable) {
        return orderRepository.findByUserId(userId, pageable)
                .map(this::toResponse);
    }

    /**
     * Get user's open orders
     */
    @Transactional(readOnly = true)
    public List<OrderResponse> getOpenOrders(UUID userId) {
        List<Order> openOrders = orderRepository.findByUserIdAndStatus(userId, OrderStatus.OPEN);
        openOrders.addAll(orderRepository.findByUserIdAndStatus(userId, OrderStatus.PARTIALLY_FILLED));

        return openOrders.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get open orders for a symbol (for order book display)
     */
    @Transactional(readOnly = true)
    public List<OrderResponse> getOpenOrdersBySymbol(String symbol) {
        return orderRepository.findOpenOrdersBySymbol(symbol.toUpperCase()).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Convert Order entity to OrderResponse DTO
     */
    private OrderResponse toResponse(Order order) {
        return OrderResponse.builder()
                .orderId(order.getOrderId())
                .userId(order.getUserId())
                .symbol(order.getSymbol())
                .side(order.getSide())
                .type(order.getType())
                .price(order.getPrice())
                .quantity(order.getQuantity())
                .filledQuantity(order.getFilledQuantity())
                .remainingQuantity(order.getRemainingQuantity())
                .avgFillPrice(order.getAvgFillPrice())
                .status(order.getStatus())
                .clientOrderId(order.getClientOrderId())
                .rejectReason(order.getRejectReason())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .filledAt(order.getFilledAt())
                .build();
    }
}

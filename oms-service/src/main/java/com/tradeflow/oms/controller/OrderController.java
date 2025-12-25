package com.tradeflow.oms.controller;

import com.tradeflow.oms.dto.CancelOrderRequest;
import com.tradeflow.oms.dto.OrderResponse;
import com.tradeflow.oms.dto.PlaceOrderRequest;
import com.tradeflow.oms.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST Controller for Order operations
 */
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Slf4j
public class OrderController {

    private final OrderService orderService;

    /**
     * Place a new order
     */
    @PostMapping
    public ResponseEntity<OrderResponse> placeOrder(
            @RequestHeader("X-User-Id") UUID userId,
            @Valid @RequestBody PlaceOrderRequest request) {
        log.info("User {} placing order: {} {} {}",
                userId, request.getSide(), request.getQuantity(), request.getSymbol());

        OrderResponse response = orderService.placeOrder(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Cancel an order
     */
    @DeleteMapping("/{orderId}")
    public ResponseEntity<OrderResponse> cancelOrder(
            @RequestHeader("X-User-Id") UUID userId,
            @PathVariable UUID orderId,
            @RequestParam(required = false) String reason) {
        log.info("User {} cancelling order {}", userId, orderId);

        CancelOrderRequest request = CancelOrderRequest.builder()
                .orderId(orderId)
                .reason(reason)
                .build();

        OrderResponse response = orderService.cancelOrder(userId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Get order by ID
     */
    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponse> getOrder(
            @RequestHeader("X-User-Id") UUID userId,
            @PathVariable UUID orderId) {
        log.debug("User {} fetching order {}", userId, orderId);

        OrderResponse response = orderService.getOrder(userId, orderId);
        return ResponseEntity.ok(response);
    }

    /**
     * Get user's orders with pagination
     */
    @GetMapping
    public ResponseEntity<Page<OrderResponse>> getUserOrders(
            @RequestHeader("X-User-Id") UUID userId,
            @PageableDefault(size = 20) Pageable pageable) {
        log.debug("User {} fetching orders, page {}", userId, pageable.getPageNumber());

        Page<OrderResponse> orders = orderService.getUserOrders(userId, pageable);
        return ResponseEntity.ok(orders);
    }

    /**
     * Get user's open orders
     */
    @GetMapping("/open")
    public ResponseEntity<List<OrderResponse>> getOpenOrders(
            @RequestHeader("X-User-Id") UUID userId) {
        log.debug("User {} fetching open orders", userId);

        List<OrderResponse> orders = orderService.getOpenOrders(userId);
        return ResponseEntity.ok(orders);
    }

    /**
     * Get open orders for a symbol (for order book)
     */
    @GetMapping("/symbol/{symbol}")
    public ResponseEntity<List<OrderResponse>> getOpenOrdersBySymbol(
            @PathVariable String symbol) {
        log.debug("Fetching open orders for symbol {}", symbol);

        List<OrderResponse> orders = orderService.getOpenOrdersBySymbol(symbol);
        return ResponseEntity.ok(orders);
    }
}

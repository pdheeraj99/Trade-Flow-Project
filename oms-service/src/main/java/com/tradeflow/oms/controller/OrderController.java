package com.tradeflow.oms.controller;

import com.tradeflow.oms.dto.CancelOrderRequest;
import com.tradeflow.oms.dto.OrderResponse;
import com.tradeflow.oms.dto.PlaceOrderRequest;
import com.tradeflow.oms.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
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
@Tag(name = "Orders", description = "Order management and trading operations")
public class OrderController {

    private final OrderService orderService;

    /**
     * Place a new order
     */
    @Operation(summary = "Place new order", description = "Submit a new trading order (LIMIT or MARKET)")
    @ApiResponse(responseCode = "201", description = "Order placed successfully")
    @ApiResponse(responseCode = "400", description = "Invalid order data")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @ApiResponse(responseCode = "422", description = "Insufficient funds or order rejected")
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<OrderResponse> placeOrder(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody PlaceOrderRequest request) {
        UUID userId = UUID.fromString(jwt.getClaimAsString("userId"));
        log.info("User {} placing order: {} {} {}",
                userId, request.getSide(), request.getQuantity(), request.getSymbol());

        OrderResponse response = orderService.placeOrder(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Cancel an order
     */
    @Operation(summary = "Cancel order", description = "Cancel an existing open order")
    @ApiResponse(responseCode = "200", description = "Order cancelled successfully")
    @ApiResponse(responseCode = "400", description = "Order cannot be cancelled")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @ApiResponse(responseCode = "404", description = "Order not found")
    @DeleteMapping("/{orderId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<OrderResponse> cancelOrder(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID orderId,
            @RequestParam(required = false) String reason) {
        UUID userId = UUID.fromString(jwt.getClaimAsString("userId"));
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
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<OrderResponse> getOrder(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID orderId) {
        UUID userId = UUID.fromString(jwt.getClaimAsString("userId"));
        log.debug("User {} fetching order {}", userId, orderId);

        OrderResponse response = orderService.getOrder(userId, orderId);
        return ResponseEntity.ok(response);
    }

    /**
     * Get user's orders with pagination
     */
    @GetMapping
    public ResponseEntity<Page<OrderResponse>> getUserOrders(
            @AuthenticationPrincipal Jwt jwt,
            @PageableDefault(size = 20) Pageable pageable) {
        UUID userId = UUID.fromString(jwt.getClaimAsString("userId"));
        log.debug("User {} fetching orders, page {}", userId, pageable.getPageNumber());

        Page<OrderResponse> orders = orderService.getUserOrders(userId, pageable);
        return ResponseEntity.ok(orders);
    }

    /**
     * Get user's open orders
     */
    @GetMapping("/open")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<OrderResponse>> getOpenOrders(
            @AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getClaimAsString("userId"));
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

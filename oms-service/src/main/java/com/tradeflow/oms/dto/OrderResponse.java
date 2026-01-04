package com.tradeflow.oms.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.tradeflow.common.enums.OrderSide;
import com.tradeflow.common.enums.OrderStatus;
import com.tradeflow.common.enums.OrderType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO for order details
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Order details with execution status")
public class OrderResponse {

    @Schema(description = "Order ID", example = "123e4567-e89b-12d3-a456-426614174000")
    private UUID orderId;

    @Schema(description = "User ID", example = "123e4567-e89b-12d3-a456-426614174000")
    private UUID userId;

    @Schema(description = "Trading symbol", example = "BTCUSDT")
    private String symbol;

    @Schema(description = "Order side", example = "BUY", allowableValues = {"BUY", "SELL"})
    private OrderSide side;

    @Schema(description = "Order type", example = "LIMIT", allowableValues = {"LIMIT", "MARKET"})
    private OrderType type;

    @Schema(description = "Order price in quote currency", type = "string", example = "50000.12345678")
    @JsonSerialize(using = ToStringSerializer.class)
    private BigDecimal price;

    @Schema(description = "Total order quantity in base currency", type = "string", example = "1.50000000")
    @JsonSerialize(using = ToStringSerializer.class)
    private BigDecimal quantity;

    @Schema(description = "Filled quantity in base currency", type = "string", example = "0.75000000")
    @JsonSerialize(using = ToStringSerializer.class)
    private BigDecimal filledQuantity;

    @Schema(description = "Remaining quantity to be filled", type = "string", example = "0.75000000")
    @JsonSerialize(using = ToStringSerializer.class)
    private BigDecimal remainingQuantity;

    @Schema(description = "Average fill price", type = "string", example = "50100.50000000")
    @JsonSerialize(using = ToStringSerializer.class)
    private BigDecimal avgFillPrice;

    @Schema(description = "Order status", example = "OPEN", allowableValues = {"PENDING_VALIDATION", "FUNDS_RESERVED", "OPEN", "PARTIALLY_FILLED", "FILLED", "CANCELLED", "REJECTED", "EXPIRED"})
    private OrderStatus status;

    @Schema(description = "Client-provided order ID", example = "client-order-12345")
    private String clientOrderId;

    @Schema(description = "Reason for rejection (if applicable)", example = "Insufficient funds")
    private String rejectReason;

    @Schema(description = "Order creation timestamp", example = "2026-01-03T10:15:00Z")
    private Instant createdAt;

    @Schema(description = "Order last update timestamp", example = "2026-01-03T10:20:00Z")
    private Instant updatedAt;

    @Schema(description = "Order filled timestamp", example = "2026-01-03T10:20:00Z")
    private Instant filledAt;
}

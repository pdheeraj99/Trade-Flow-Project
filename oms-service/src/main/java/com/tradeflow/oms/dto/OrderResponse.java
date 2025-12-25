package com.tradeflow.oms.dto;

import com.tradeflow.common.enums.OrderSide;
import com.tradeflow.common.enums.OrderStatus;
import com.tradeflow.common.enums.OrderType;
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
public class OrderResponse {

    private UUID orderId;
    private UUID userId;
    private String symbol;
    private OrderSide side;
    private OrderType type;
    private BigDecimal price;
    private BigDecimal quantity;
    private BigDecimal filledQuantity;
    private BigDecimal remainingQuantity;
    private BigDecimal avgFillPrice;
    private OrderStatus status;
    private String clientOrderId;
    private String rejectReason;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant filledAt;
}

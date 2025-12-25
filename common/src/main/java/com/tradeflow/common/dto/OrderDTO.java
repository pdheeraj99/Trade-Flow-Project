package com.tradeflow.common.dto;

import com.tradeflow.common.enums.OrderSide;
import com.tradeflow.common.enums.OrderStatus;
import com.tradeflow.common.enums.OrderType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Order Data Transfer Object
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderDTO {

    private UUID orderId;

    @NotNull(message = "User ID is required")
    private UUID userId;

    @NotBlank(message = "Symbol is required")
    private String symbol;

    @NotNull(message = "Order side is required")
    private OrderSide side;

    @NotNull(message = "Order type is required")
    private OrderType type;

    @DecimalMin(value = "0.0", inclusive = false, message = "Price must be positive")
    private BigDecimal price;

    @NotNull(message = "Quantity is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Quantity must be positive")
    private BigDecimal quantity;

    private BigDecimal filledQuantity;

    private OrderStatus status;

    private Instant createdAt;

    private Instant updatedAt;
}

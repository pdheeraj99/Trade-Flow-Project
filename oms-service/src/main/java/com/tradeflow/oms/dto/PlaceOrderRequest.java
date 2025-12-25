package com.tradeflow.oms.dto;

import com.tradeflow.common.enums.OrderSide;
import com.tradeflow.common.enums.OrderType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Request DTO for placing an order
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlaceOrderRequest {

    @NotBlank(message = "Symbol is required")
    @Size(max = 20, message = "Symbol cannot exceed 20 characters")
    private String symbol;

    @NotNull(message = "Order side is required")
    private OrderSide side;

    @NotNull(message = "Order type is required")
    private OrderType type;

    /**
     * Price for limit orders (required for LIMIT, ignored for MARKET)
     */
    @DecimalMin(value = "0.0", inclusive = false, message = "Price must be positive")
    private BigDecimal price;

    /**
     * Quantity to buy/sell
     */
    @NotNull(message = "Quantity is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Quantity must be positive")
    private BigDecimal quantity;

    /**
     * Client-provided order ID for idempotency
     */
    @Size(max = 50, message = "Client order ID cannot exceed 50 characters")
    private String clientOrderId;
}

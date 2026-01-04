package com.tradeflow.oms.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.tradeflow.common.enums.OrderSide;
import com.tradeflow.common.enums.OrderType;
import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "Request to place a new trading order")
public class PlaceOrderRequest {

    @Schema(description = "Trading symbol", example = "BTCUSDT", required = true)
    @NotBlank(message = "Symbol is required")
    @Size(max = 20, message = "Symbol cannot exceed 20 characters")
    private String symbol;

    @Schema(description = "Order side", example = "BUY", allowableValues = {"BUY", "SELL"}, required = true)
    @NotNull(message = "Order side is required")
    private OrderSide side;

    @Schema(description = "Order type", example = "LIMIT", allowableValues = {"LIMIT", "MARKET"}, required = true)
    @NotNull(message = "Order type is required")
    private OrderType type;

    /**
     * Price for limit orders (required for LIMIT, ignored for MARKET)
     */
    @Schema(description = "Price in quote currency (required for LIMIT orders)", type = "string", example = "50000.12345678")
    @DecimalMin(value = "0.0", inclusive = false, message = "Price must be positive")
    @JsonSerialize(using = ToStringSerializer.class)
    private BigDecimal price;

    /**
     * Quantity to buy/sell
     */
    @Schema(description = "Quantity in base currency", type = "string", example = "1.50000000", required = true)
    @NotNull(message = "Quantity is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Quantity must be positive")
    @JsonSerialize(using = ToStringSerializer.class)
    private BigDecimal quantity;

    /**
     * Client-provided order ID for idempotency
     */
    @Schema(description = "Client-provided order ID for idempotency", example = "client-order-12345")
    @Size(max = 50, message = "Client order ID cannot exceed 50 characters")
    private String clientOrderId;
}

package com.tradeflow.common.event;

import com.tradeflow.common.enums.OrderSide;
import com.tradeflow.common.enums.OrderType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Event representing an order sent from OMS to the Matching Engine via RabbitMQ.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderToMatchingEvent {
    private UUID orderId;
    private UUID userId;
    private String symbol;
    private OrderSide side;
    private OrderType type;
    private BigDecimal price;
    private BigDecimal quantity;
    private Instant timestamp;
}

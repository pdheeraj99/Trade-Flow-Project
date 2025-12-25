package com.tradeflow.common.event;

import com.tradeflow.common.enums.OrderSide;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Event published when a trade is executed by the Matching Engine
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TradeExecutedEvent {

    private UUID eventId;

    private UUID tradeId;

    private String symbol;

    // Buy side
    private UUID buyOrderId;
    private UUID buyerId;

    // Sell side
    private UUID sellOrderId;
    private UUID sellerId;

    // Trade details
    private BigDecimal price;
    private BigDecimal quantity;
    private BigDecimal quoteAmount;

    private OrderSide takerSide;

    private Instant executedAt;

    private Instant eventTimestamp;
}

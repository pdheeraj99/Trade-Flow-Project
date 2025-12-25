package com.tradeflow.common.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Event published when a trade is executed by the Matching Engine.
 * Published to Kafka for OMS and other services to consume.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TradeExecutedEvent {

    private UUID tradeId;

    private String symbol;

    // Buy side
    private UUID buyOrderId;
    private UUID buyUserId;

    // Sell side
    private UUID sellOrderId;
    private UUID sellUserId;

    // Maker/Taker info
    private UUID makerOrderId;
    private UUID takerOrderId;

    // Trade details
    private BigDecimal price;
    private BigDecimal quantity;

    private Instant timestamp;
}

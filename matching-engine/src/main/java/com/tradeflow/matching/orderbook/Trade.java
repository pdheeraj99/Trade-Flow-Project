package com.tradeflow.matching.orderbook;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Represents a trade execution between two orders.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Trade {

    private UUID tradeId;
    private String symbol;

    // Maker is the order that was resting in the book
    private UUID makerOrderId;
    private UUID makerUserId;

    // Taker is the incoming order that matched
    private UUID takerOrderId;
    private UUID takerUserId;

    // Buy/Sell determination
    private UUID buyOrderId;
    private UUID buyUserId;
    private UUID sellOrderId;
    private UUID sellUserId;

    private BigDecimal price; // Execution price (maker's price)
    private BigDecimal quantity; // Execution quantity
    private Instant timestamp;
}

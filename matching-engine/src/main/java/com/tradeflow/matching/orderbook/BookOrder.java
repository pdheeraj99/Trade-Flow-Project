package com.tradeflow.matching.orderbook;

import com.tradeflow.common.enums.OrderSide;
import com.tradeflow.common.enums.OrderType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Internal order representation in the order book.
 * Immutable snapshots are created for each state change.
 */
@Data
@Builder(toBuilder = true)
public class BookOrder {

    private final UUID orderId;
    private final UUID userId;
    private final String symbol;
    private final OrderSide side;
    private final OrderType type;
    private final BigDecimal price;
    private final BigDecimal originalQuantity;
    private final BigDecimal remainingQuantity;
    private final Instant timestamp;
    private final long sequenceNumber; // For FIFO ordering at same price

    /**
     * Check if order is fully filled
     */
    public boolean isFullyFilled() {
        return remainingQuantity.compareTo(BigDecimal.ZERO) <= 0;
    }

    /**
     * Create a new order with reduced quantity after partial fill
     */
    public BookOrder withReducedQuantity(BigDecimal filledQuantity) {
        return this.toBuilder()
                .remainingQuantity(remainingQuantity.subtract(filledQuantity))
                .build();
    }

    /**
     * Check if this order can match with another order (price compatibility)
     */
    public boolean canMatchWith(BookOrder other) {
        if (this.side == other.side) {
            return false; // Same side orders don't match
        }

        if (this.side == OrderSide.BUY) {
            // Buy order: matches if buy price >= sell price
            return this.price.compareTo(other.price) >= 0;
        } else {
            // Sell order: matches if sell price <= buy price
            return this.price.compareTo(other.price) <= 0;
        }
    }
}

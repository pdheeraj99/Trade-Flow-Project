package com.tradeflow.matching.orderbook;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * Result of matching an incoming order against the order book.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MatchResult {

    /**
     * The incoming order (updated with remaining quantity)
     */
    private BookOrder remainingOrder;

    /**
     * List of trades executed
     */
    private List<Trade> trades;

    /**
     * Whether the order was fully filled
     */
    private boolean fullyFilled;

    /**
     * Total quantity filled
     */
    private BigDecimal filledQuantity;

    /**
     * Average execution price
     */
    private BigDecimal avgPrice;

    /**
     * Check if any trades occurred
     */
    public boolean hasTrades() {
        return trades != null && !trades.isEmpty();
    }
}

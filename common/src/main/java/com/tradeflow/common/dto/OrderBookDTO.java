package com.tradeflow.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * Order Book Data Transfer Object for WebSocket broadcast
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderBookDTO {

    private String symbol;

    private List<PriceLevel> bids; // Sorted descending by price

    private List<PriceLevel> asks; // Sorted ascending by price

    private long lastUpdateId;

    private long timestamp;

    /**
     * Single price level in the order book
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PriceLevel {
        private BigDecimal price;
        private BigDecimal quantity;
    }
}

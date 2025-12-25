package com.tradeflow.common.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Event published when the order book changes.
 * Published by Matching Engine to Kafka.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderBookUpdateEvent {

    private String symbol;

    private List<PriceLevel> bids;

    private List<PriceLevel> asks;

    private Instant timestamp;

    /**
     * Price level with aggregated quantity
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PriceLevel {
        private BigDecimal price;
        private BigDecimal quantity;
    }
}

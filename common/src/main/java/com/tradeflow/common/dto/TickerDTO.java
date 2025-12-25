package com.tradeflow.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Market ticker data transfer object
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TickerDTO {

    private String symbol;

    private BigDecimal lastPrice;

    private BigDecimal bidPrice;

    private BigDecimal askPrice;

    private BigDecimal high24h;

    private BigDecimal low24h;

    private BigDecimal volume24h;

    private BigDecimal priceChange24h;

    private BigDecimal priceChangePercent24h;

    private Instant timestamp;

    private boolean stale; // True if data is older than threshold
}

package com.tradeflow.marketdata.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Ticker DTO for real-time price updates
 */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class TickerResponse {

    private String symbol;
    private String coinId;
    private BigDecimal price;
    private BigDecimal change24h;
    private BigDecimal changePercent24h;
    private BigDecimal high24h;
    private BigDecimal low24h;
    private BigDecimal volume24h;
    private Instant timestamp;
    private boolean stale; // True if data is older than cache TTL
}

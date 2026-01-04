package com.tradeflow.marketdata.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "Real-time cryptocurrency ticker data")
public class TickerResponse {

    @Schema(description = "Trading symbol", example = "BTCUSDT")
    private String symbol;

    @Schema(description = "CoinGecko coin ID", example = "bitcoin")
    private String coinId;

    @Schema(description = "Current price", type = "string", example = "50000.12345678")
    @JsonSerialize(using = ToStringSerializer.class)
    private BigDecimal price;

    @Schema(description = "24-hour price change", type = "string", example = "1250.50000000")
    @JsonSerialize(using = ToStringSerializer.class)
    private BigDecimal change24h;

    @Schema(description = "24-hour price change percentage", type = "string", example = "2.56")
    @JsonSerialize(using = ToStringSerializer.class)
    private BigDecimal changePercent24h;

    @Schema(description = "24-hour high price", type = "string", example = "51000.00000000")
    @JsonSerialize(using = ToStringSerializer.class)
    private BigDecimal high24h;

    @Schema(description = "24-hour low price", type = "string", example = "48500.00000000")
    @JsonSerialize(using = ToStringSerializer.class)
    private BigDecimal low24h;

    @Schema(description = "24-hour trading volume", type = "string", example = "125000.00000000")
    @JsonSerialize(using = ToStringSerializer.class)
    private BigDecimal volume24h;

    @Schema(description = "Timestamp of last update", example = "2026-01-03T10:15:00Z")
    private Instant timestamp;

    @Schema(description = "Data staleness flag", example = "false")
    private boolean stale;
}

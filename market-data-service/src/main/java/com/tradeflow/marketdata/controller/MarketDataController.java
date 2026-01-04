package com.tradeflow.marketdata.controller;

import com.tradeflow.marketdata.dto.TickerResponse;
import com.tradeflow.marketdata.service.MarketDataService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for Market Data endpoints
 */
@RestController
@RequestMapping("/api/market")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Market Data", description = "Real-time cryptocurrency market data and price information")
public class MarketDataController {

    private final MarketDataService marketDataService;

    /**
     * Get ticker for a trading pair
     */
    @Operation(summary = "Get ticker data", description = "Retrieve real-time ticker data for a specific trading symbol")
    @ApiResponse(responseCode = "200", description = "Ticker data retrieved successfully")
    @ApiResponse(responseCode = "404", description = "Symbol not found")
    @GetMapping("/ticker/{symbol}")
    public ResponseEntity<TickerResponse> getTicker(@PathVariable String symbol) {
        log.debug("Getting ticker for {}", symbol);
        TickerResponse ticker = marketDataService.getTicker(symbol);
        return ResponseEntity.ok(ticker);
    }

    /**
     * Get all tickers
     */
    @Operation(summary = "Get all tickers", description = "Retrieve real-time ticker data for all supported symbols")
    @ApiResponse(responseCode = "200", description = "All tickers retrieved successfully")
    @GetMapping("/tickers")
    public ResponseEntity<List<TickerResponse>> getAllTickers() {
        log.debug("Getting all tickers");
        List<TickerResponse> tickers = marketDataService.getAllTickers();
        return ResponseEntity.ok(tickers);
    }

    /**
     * Health check
     */
    @GetMapping("/health")
    public ResponseEntity<HealthStatus> health() {
        return ResponseEntity.ok(new HealthStatus("UP", System.currentTimeMillis()));
    }

    public record HealthStatus(String status, long timestamp) {
    }
}

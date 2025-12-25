package com.tradeflow.marketdata.controller;

import com.tradeflow.marketdata.dto.TickerResponse;
import com.tradeflow.marketdata.service.MarketDataService;
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
public class MarketDataController {

    private final MarketDataService marketDataService;

    /**
     * Get ticker for a trading pair
     */
    @GetMapping("/ticker/{symbol}")
    public ResponseEntity<TickerResponse> getTicker(@PathVariable String symbol) {
        log.debug("Getting ticker for {}", symbol);
        TickerResponse ticker = marketDataService.getTicker(symbol);
        return ResponseEntity.ok(ticker);
    }

    /**
     * Get all tickers
     */
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

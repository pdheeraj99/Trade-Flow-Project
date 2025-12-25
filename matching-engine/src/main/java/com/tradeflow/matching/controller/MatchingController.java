package com.tradeflow.matching.controller;

import com.tradeflow.matching.engine.MatchingEngine;
import com.tradeflow.matching.orderbook.OrderBook;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for Matching Engine monitoring and order book queries.
 * These are read-only endpoints for monitoring purposes.
 */
@RestController
@RequestMapping("/api/matching")
@RequiredArgsConstructor
@Slf4j
public class MatchingController {

    private final MatchingEngine matchingEngine;

    /**
     * Get order book snapshot for a symbol
     */
    @GetMapping("/orderbook/{symbol}")
    public ResponseEntity<OrderBook.OrderBookSnapshot> getOrderBook(
            @PathVariable String symbol,
            @RequestParam(defaultValue = "10") int depth) {
        log.debug("Getting order book for {} (depth: {})", symbol, depth);

        OrderBook.OrderBookSnapshot snapshot = matchingEngine.getSnapshot(symbol, depth);
        if (snapshot == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(snapshot);
    }

    /**
     * Get book statistics for a symbol
     */
    @GetMapping("/stats/{symbol}")
    public ResponseEntity<MatchingEngine.BookStats> getStats(@PathVariable String symbol) {
        log.debug("Getting stats for {}", symbol);

        MatchingEngine.BookStats stats = matchingEngine.getBookStats(symbol);
        if (stats == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(stats);
    }

    /**
     * Get all active trading symbols
     */
    @GetMapping("/symbols")
    public ResponseEntity<List<String>> getActiveSymbols() {
        return ResponseEntity.ok(matchingEngine.getActiveSymbols());
    }

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<HealthStatus> health() {
        return ResponseEntity.ok(new HealthStatus(
                "UP",
                matchingEngine.getActiveSymbols().size(),
                System.currentTimeMillis()));
    }

    public record HealthStatus(String status, int activeSymbols, long timestamp) {
    }
}

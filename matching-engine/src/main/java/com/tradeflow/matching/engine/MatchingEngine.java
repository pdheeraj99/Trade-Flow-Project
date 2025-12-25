package com.tradeflow.matching.engine;

import com.tradeflow.matching.orderbook.BookOrder;
import com.tradeflow.matching.orderbook.MatchResult;
import com.tradeflow.matching.orderbook.OrderBook;
import com.tradeflow.matching.orderbook.Trade;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Matching Engine managing multiple order books.
 * Each symbol has its own order book.
 * Uses Platform Threads (not Virtual Threads) for CPU-bound matching.
 */
@Component
@Slf4j
public class MatchingEngine {

    // Order books per symbol
    private final Map<String, OrderBook> orderBooks = new ConcurrentHashMap<>();

    /**
     * Process an incoming order
     */
    public MatchResult processOrder(BookOrder order) {
        log.info("Processing order: {} {} {} @ {} ({})",
                order.getOrderId(), order.getSide(), order.getOriginalQuantity(),
                order.getPrice(), order.getSymbol());

        OrderBook book = getOrCreateOrderBook(order.getSymbol());
        MatchResult result = book.addOrder(order);

        if (result.hasTrades()) {
            log.info("Order {} matched: {} trades, filled {}",
                    order.getOrderId(), result.getTrades().size(), result.getFilledQuantity());
        } else {
            log.info("Order {} added to book (no match)", order.getOrderId());
        }

        return result;
    }

    /**
     * Cancel an order from the book
     */
    public boolean cancelOrder(String symbol, UUID orderId, com.tradeflow.common.enums.OrderSide side) {
        OrderBook book = orderBooks.get(symbol.toUpperCase());
        if (book == null) {
            log.warn("Order book not found for symbol: {}", symbol);
            return false;
        }
        return book.cancelOrder(orderId, side);
    }

    /**
     * Get or create order book for symbol
     */
    private OrderBook getOrCreateOrderBook(String symbol) {
        return orderBooks.computeIfAbsent(symbol.toUpperCase(), OrderBook::new);
    }

    /**
     * Get order book snapshot
     */
    public OrderBook.OrderBookSnapshot getSnapshot(String symbol, int depth) {
        OrderBook book = orderBooks.get(symbol.toUpperCase());
        if (book == null) {
            return null;
        }
        return book.getSnapshot(depth);
    }

    /**
     * Get all active symbols
     */
    public List<String> getActiveSymbols() {
        return List.copyOf(orderBooks.keySet());
    }

    /**
     * Get book statistics
     */
    public BookStats getBookStats(String symbol) {
        OrderBook book = orderBooks.get(symbol.toUpperCase());
        if (book == null) {
            return null;
        }
        return new BookStats(
                symbol,
                book.getBestBid(),
                book.getBestAsk(),
                book.getSpread(),
                book.getBidDepth(),
                book.getAskDepth());
    }

    public record BookStats(
            String symbol,
            java.math.BigDecimal bestBid,
            java.math.BigDecimal bestAsk,
            java.math.BigDecimal spread,
            int bidDepth,
            int askDepth) {
    }
}

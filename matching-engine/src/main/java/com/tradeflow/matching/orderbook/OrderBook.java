package com.tradeflow.matching.orderbook;

import com.tradeflow.common.enums.OrderSide;
import com.tradeflow.common.enums.OrderType;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * In-memory Order Book for a single trading symbol.
 * Implements price-time priority matching (FIFO at each price level).
 * 
 * Thread-safe using ConcurrentSkipListMap for price levels.
 * Uses Platform Threads for CPU-bound matching (not Virtual Threads).
 */
@Slf4j
public class OrderBook {

    @Getter
    private final String symbol;

    // Bids: highest price first (descending)
    private final ConcurrentSkipListMap<BigDecimal, LinkedList<BookOrder>> bids = new ConcurrentSkipListMap<>(
            Comparator.reverseOrder());

    // Asks: lowest price first (ascending)
    private final ConcurrentSkipListMap<BigDecimal, LinkedList<BookOrder>> asks = new ConcurrentSkipListMap<>();

    // Sequence number for FIFO ordering
    private final AtomicLong sequenceGenerator = new AtomicLong(0);

    private static final int SCALE = 8;

    public OrderBook(String symbol) {
        this.symbol = symbol;
        log.info("OrderBook created for symbol: {}", symbol);
    }

    /**
     * Add an order to the book and attempt matching.
     * Returns match result with any trades executed.
     */
    public synchronized MatchResult addOrder(BookOrder incomingOrder) {
        log.debug("Adding order to book: {} {} {} @ {}",
                incomingOrder.getSide(), incomingOrder.getOriginalQuantity(),
                symbol, incomingOrder.getPrice());

        // Assign sequence number for FIFO ordering
        BookOrder order = incomingOrder.toBuilder()
                .sequenceNumber(sequenceGenerator.incrementAndGet())
                .build();

        // For market orders, match immediately
        if (order.getType() == OrderType.MARKET) {
            return matchMarketOrder(order);
        }

        // For limit orders, attempt matching then add remaining to book
        return matchLimitOrder(order);
    }

    /**
     * Match a market order against the book (aggressive matching)
     */
    private MatchResult matchMarketOrder(BookOrder order) {
        List<Trade> trades = new ArrayList<>();
        BigDecimal remainingQty = order.getRemainingQuantity();
        BigDecimal totalFilled = BigDecimal.ZERO;
        BigDecimal totalValue = BigDecimal.ZERO;

        // Get opposite side book
        ConcurrentSkipListMap<BigDecimal, LinkedList<BookOrder>> oppositeBook = order.getSide() == OrderSide.BUY ? asks
                : bids;

        Iterator<Map.Entry<BigDecimal, LinkedList<BookOrder>>> priceIterator = oppositeBook.entrySet().iterator();

        while (priceIterator.hasNext() && remainingQty.compareTo(BigDecimal.ZERO) > 0) {
            Map.Entry<BigDecimal, LinkedList<BookOrder>> priceLevel = priceIterator.next();
            BigDecimal price = priceLevel.getKey();
            LinkedList<BookOrder> ordersAtPrice = priceLevel.getValue();

            Iterator<BookOrder> orderIterator = ordersAtPrice.iterator();
            while (orderIterator.hasNext() && remainingQty.compareTo(BigDecimal.ZERO) > 0) {
                BookOrder makerOrder = orderIterator.next();

                // Calculate fill quantity
                BigDecimal fillQty = remainingQty.min(makerOrder.getRemainingQuantity());

                // Create trade
                Trade trade = createTrade(order, makerOrder, price, fillQty);
                trades.add(trade);

                // Update quantities
                remainingQty = remainingQty.subtract(fillQty);
                totalFilled = totalFilled.add(fillQty);
                totalValue = totalValue.add(price.multiply(fillQty));

                // Remove or update maker order
                if (makerOrder.getRemainingQuantity().subtract(fillQty).compareTo(BigDecimal.ZERO) <= 0) {
                    orderIterator.remove();
                } else {
                    // Update maker order in place
                    BookOrder updatedMaker = makerOrder.withReducedQuantity(fillQty);
                    orderIterator.remove();
                    ordersAtPrice.addFirst(updatedMaker); // Re-add at front
                    break; // Move to result as maker has remaining
                }
            }

            // Remove empty price level
            if (ordersAtPrice.isEmpty()) {
                priceIterator.remove();
            }
        }

        // Calculate average price
        BigDecimal avgPrice = totalFilled.compareTo(BigDecimal.ZERO) > 0
                ? totalValue.divide(totalFilled, SCALE, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        return MatchResult.builder()
                .remainingOrder(order.toBuilder().remainingQuantity(remainingQty).build())
                .trades(trades)
                .fullyFilled(remainingQty.compareTo(BigDecimal.ZERO) <= 0)
                .filledQuantity(totalFilled)
                .avgPrice(avgPrice)
                .build();
    }

    /**
     * Match a limit order and add remainder to book
     */
    private MatchResult matchLimitOrder(BookOrder order) {
        List<Trade> trades = new ArrayList<>();
        BigDecimal remainingQty = order.getRemainingQuantity();
        BigDecimal totalFilled = BigDecimal.ZERO;
        BigDecimal totalValue = BigDecimal.ZERO;

        // Get opposite side book
        ConcurrentSkipListMap<BigDecimal, LinkedList<BookOrder>> oppositeBook = order.getSide() == OrderSide.BUY ? asks
                : bids;

        Iterator<Map.Entry<BigDecimal, LinkedList<BookOrder>>> priceIterator = oppositeBook.entrySet().iterator();

        while (priceIterator.hasNext() && remainingQty.compareTo(BigDecimal.ZERO) > 0) {
            Map.Entry<BigDecimal, LinkedList<BookOrder>> priceLevel = priceIterator.next();
            BigDecimal price = priceLevel.getKey();

            // Check price compatibility
            boolean priceCompatible = order.getSide() == OrderSide.BUY
                    ? order.getPrice().compareTo(price) >= 0 // Buy: willing to pay >= ask
                    : order.getPrice().compareTo(price) <= 0; // Sell: willing to accept <= bid

            if (!priceCompatible) {
                break; // No more matches possible
            }

            LinkedList<BookOrder> ordersAtPrice = priceLevel.getValue();
            Iterator<BookOrder> orderIterator = ordersAtPrice.iterator();

            while (orderIterator.hasNext() && remainingQty.compareTo(BigDecimal.ZERO) > 0) {
                BookOrder makerOrder = orderIterator.next();

                // Calculate fill quantity
                BigDecimal fillQty = remainingQty.min(makerOrder.getRemainingQuantity());

                // Create trade (at maker's price)
                Trade trade = createTrade(order, makerOrder, price, fillQty);
                trades.add(trade);

                // Update quantities
                remainingQty = remainingQty.subtract(fillQty);
                totalFilled = totalFilled.add(fillQty);
                totalValue = totalValue.add(price.multiply(fillQty));

                // Remove or update maker order
                BigDecimal makerRemaining = makerOrder.getRemainingQuantity().subtract(fillQty);
                if (makerRemaining.compareTo(BigDecimal.ZERO) <= 0) {
                    orderIterator.remove();
                } else {
                    // Update maker order in place
                    BookOrder updatedMaker = makerOrder.withReducedQuantity(fillQty);
                    orderIterator.remove();
                    ordersAtPrice.addFirst(updatedMaker);
                    break;
                }
            }

            // Remove empty price level
            if (ordersAtPrice.isEmpty()) {
                priceIterator.remove();
            }
        }

        // Add remaining quantity to book
        if (remainingQty.compareTo(BigDecimal.ZERO) > 0) {
            BookOrder remainingOrder = order.toBuilder()
                    .remainingQuantity(remainingQty)
                    .build();
            addToBook(remainingOrder);
        }

        // Calculate average price
        BigDecimal avgPrice = totalFilled.compareTo(BigDecimal.ZERO) > 0
                ? totalValue.divide(totalFilled, SCALE, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        return MatchResult.builder()
                .remainingOrder(order.toBuilder().remainingQuantity(remainingQty).build())
                .trades(trades)
                .fullyFilled(remainingQty.compareTo(BigDecimal.ZERO) <= 0)
                .filledQuantity(totalFilled)
                .avgPrice(avgPrice)
                .build();
    }

    /**
     * Add order to the appropriate side of the book
     */
    private void addToBook(BookOrder order) {
        ConcurrentSkipListMap<BigDecimal, LinkedList<BookOrder>> book = order.getSide() == OrderSide.BUY ? bids : asks;

        book.computeIfAbsent(order.getPrice(), k -> new LinkedList<>()).addLast(order);

        log.debug("Added to book: {} {} @ {} (remaining: {})",
                order.getSide(), symbol, order.getPrice(), order.getRemainingQuantity());
    }

    /**
     * Cancel an order from the book
     */
    public synchronized boolean cancelOrder(UUID orderId, OrderSide side) {
        ConcurrentSkipListMap<BigDecimal, LinkedList<BookOrder>> book = side == OrderSide.BUY ? bids : asks;

        for (Map.Entry<BigDecimal, LinkedList<BookOrder>> entry : book.entrySet()) {
            LinkedList<BookOrder> orders = entry.getValue();
            Iterator<BookOrder> iterator = orders.iterator();

            while (iterator.hasNext()) {
                BookOrder order = iterator.next();
                if (order.getOrderId().equals(orderId)) {
                    iterator.remove();

                    // Clean up empty price level
                    if (orders.isEmpty()) {
                        book.remove(entry.getKey());
                    }

                    log.info("Cancelled order {} from book", orderId);
                    return true;
                }
            }
        }

        log.warn("Order {} not found in book for cancellation", orderId);
        return false;
    }

    /**
     * Create a trade record
     */
    private Trade createTrade(BookOrder taker, BookOrder maker, BigDecimal price, BigDecimal quantity) {
        UUID buyOrderId, buyUserId, sellOrderId, sellUserId;

        if (taker.getSide() == OrderSide.BUY) {
            buyOrderId = taker.getOrderId();
            buyUserId = taker.getUserId();
            sellOrderId = maker.getOrderId();
            sellUserId = maker.getUserId();
        } else {
            buyOrderId = maker.getOrderId();
            buyUserId = maker.getUserId();
            sellOrderId = taker.getOrderId();
            sellUserId = taker.getUserId();
        }

        return Trade.builder()
                .tradeId(UUID.randomUUID())
                .symbol(symbol)
                .makerOrderId(maker.getOrderId())
                .makerUserId(maker.getUserId())
                .takerOrderId(taker.getOrderId())
                .takerUserId(taker.getUserId())
                .buyOrderId(buyOrderId)
                .buyUserId(buyUserId)
                .sellOrderId(sellOrderId)
                .sellUserId(sellUserId)
                .price(price)
                .quantity(quantity)
                .timestamp(Instant.now())
                .build();
    }

    /**
     * Get best bid price
     */
    public BigDecimal getBestBid() {
        Map.Entry<BigDecimal, LinkedList<BookOrder>> entry = bids.firstEntry();
        return entry != null ? entry.getKey() : null;
    }

    /**
     * Get best ask price
     */
    public BigDecimal getBestAsk() {
        Map.Entry<BigDecimal, LinkedList<BookOrder>> entry = asks.firstEntry();
        return entry != null ? entry.getKey() : null;
    }

    /**
     * Get spread (best ask - best bid)
     */
    public BigDecimal getSpread() {
        BigDecimal bestBid = getBestBid();
        BigDecimal bestAsk = getBestAsk();

        if (bestBid != null && bestAsk != null) {
            return bestAsk.subtract(bestBid);
        }
        return null;
    }

    /**
     * Get book depth (number of price levels)
     */
    public int getBidDepth() {
        return bids.size();
    }

    public int getAskDepth() {
        return asks.size();
    }

    /**
     * Get snapshot of top N price levels for each side
     */
    public OrderBookSnapshot getSnapshot(int depth) {
        List<PriceLevel> bidLevels = new ArrayList<>();
        List<PriceLevel> askLevels = new ArrayList<>();

        int count = 0;
        for (Map.Entry<BigDecimal, LinkedList<BookOrder>> entry : bids.entrySet()) {
            if (count++ >= depth)
                break;
            BigDecimal totalQty = entry.getValue().stream()
                    .map(BookOrder::getRemainingQuantity)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            bidLevels.add(new PriceLevel(entry.getKey(), totalQty, entry.getValue().size()));
        }

        count = 0;
        for (Map.Entry<BigDecimal, LinkedList<BookOrder>> entry : asks.entrySet()) {
            if (count++ >= depth)
                break;
            BigDecimal totalQty = entry.getValue().stream()
                    .map(BookOrder::getRemainingQuantity)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            askLevels.add(new PriceLevel(entry.getKey(), totalQty, entry.getValue().size()));
        }

        return new OrderBookSnapshot(symbol, bidLevels, askLevels, Instant.now());
    }

    /**
     * Price level summary
     */
    public record PriceLevel(BigDecimal price, BigDecimal quantity, int orderCount) {
    }

    /**
     * Order book snapshot
     */
    public record OrderBookSnapshot(
            String symbol,
            List<PriceLevel> bids,
            List<PriceLevel> asks,
            Instant timestamp) {
    }
}

package com.tradeflow.common.constants;

/**
 * Kafka topic constants for event streaming
 */
public final class KafkaTopics {

    private KafkaTopics() {
        // Utility class - prevent instantiation
    }

    // Order processing
    public static final String ORDERS_INCOMING = "orders.incoming";
    public static final String ORDERS_TO_MATCHING = "orders.to-matching"; // OMS -> Matching Engine
    public static final String ORDERS_MATCHED = "orders.matched";
    public static final String ORDERS_REJECTED = "orders.rejected";

    // Trade events
    public static final String TRADES_EXECUTED = "trades.executed";

    // Order book updates
    public static final String ORDERBOOK_UPDATES = "orderbook.updates";

    // Market data
    public static final String TICKER_UPDATES = "ticker.updates";

    // Audit events (wire tap)
    public static final String AUDIT_EVENTS = "audit.events";
}

package com.tradeflow.common.constants;

/**
 * RabbitMQ exchange, queue, and routing key constants for Saga messaging
 */
public final class RabbitMQConstants {

    private RabbitMQConstants() {
        // Utility class - prevent instantiation
    }

    // Exchanges
    public static final String WALLET_EXCHANGE = "tradeflow.wallet.exchange";
    public static final String ORDER_EXCHANGE = "tradeflow.order.exchange";

    // Queues
    public static final String WALLET_RESERVE_QUEUE = "wallet.reserve.queue";
    public static final String WALLET_RELEASE_QUEUE = "wallet.release.queue";
    public static final String WALLET_SETTLE_QUEUE = "wallet.settle.queue";
    public static final String ORDER_RESPONSE_QUEUE = "order.response.queue";
    public static final String MATCHING_ORDER_QUEUE = "matching.order.queue";

    // Routing Keys
    public static final String ROUTING_WALLET_RESERVE = "wallet.reserve";
    public static final String ROUTING_WALLET_RELEASE = "wallet.release";
    public static final String ROUTING_WALLET_SETTLE = "wallet.settle";
    public static final String ROUTING_ORDER_RESPONSE = "order.response";
    public static final String ROUTING_ORDER_TO_MATCHING = "order.to.matching";

    // Dead Letter
    public static final String DLX_EXCHANGE = "tradeflow.dlx.exchange";
    public static final String DLQ_QUEUE = "tradeflow.dlq.queue";

    // User Events (Auth -> Wallet)
    public static final String USER_EXCHANGE = "tradeflow.user.exchange";
    public static final String USER_CREATED_QUEUE = "user.created.queue";
    public static final String ROUTING_USER_CREATED = "user.created";
}

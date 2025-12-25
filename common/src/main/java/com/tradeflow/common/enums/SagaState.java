package com.tradeflow.common.enums;

/**
 * Saga state enumeration for distributed transaction tracking
 */
public enum SagaState {
    // Initial states
    STARTED, // Saga initiated
    AWAITING_FUNDS, // Waiting for fund reservation response
    FUNDS_RESERVED, // Wallet has reserved funds/assets

    // Order processing states
    ORDER_SENT, // Order submitted to matching engine
    MATCHING_SUBMITTED, // Order in matching engine (legacy alias)
    TRADE_EXECUTED, // Trade has been matched

    // Settlement states
    SETTLEMENT_PENDING, // Awaiting settlement

    // Compensation states
    COMPENSATING, // Compensation in progress
    COMPENSATION_STARTED, // Rollback in progress (legacy alias)
    COMPENSATED, // Compensation complete

    // Terminal states
    COMPLETED, // Saga completed successfully
    FAILED // Saga failed
}

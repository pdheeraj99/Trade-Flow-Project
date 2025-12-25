package com.tradeflow.common.enums;

/**
 * Saga state enumeration for distributed transaction tracking
 */
public enum SagaState {
    STARTED, // Saga initiated
    FUNDS_RESERVED, // Wallet has reserved funds/assets
    MATCHING_SUBMITTED, // Order submitted to matching engine
    TRADE_EXECUTED, // Trade has been matched
    SETTLEMENT_PENDING, // Awaiting settlement
    COMPLETED, // Saga completed successfully
    COMPENSATION_STARTED, // Rollback in progress
    FAILED // Saga failed after compensation
}

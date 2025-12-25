package com.tradeflow.common.enums;

/**
 * Order status enumeration representing the lifecycle states
 */
public enum OrderStatus {
    PENDING_VALIDATION,  // Initial state, awaiting fund reservation
    OPEN,                // Funds reserved, submitted to matching engine
    PARTIALLY_FILLED,    // Some quantity matched
    FILLED,              // Fully matched
    CANCELLED,           // Cancelled by user or system
    REJECTED,            // Rejected due to insufficient funds or validation failure
    EXPIRED              // Order expired (for time-bound orders)
}

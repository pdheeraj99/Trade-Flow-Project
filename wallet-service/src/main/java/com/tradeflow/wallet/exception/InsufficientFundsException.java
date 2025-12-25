package com.tradeflow.wallet.exception;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Exception thrown when user has insufficient funds for an operation
 */
public class InsufficientFundsException extends RuntimeException {

    private final UUID userId;
    private final String currency;
    private final BigDecimal requested;
    private final BigDecimal available;

    public InsufficientFundsException(UUID userId, String currency, BigDecimal requested, BigDecimal available) {
        super(String.format("Insufficient funds for user %s: requested %s %s, available %s %s",
                userId, requested, currency, available, currency));
        this.userId = userId;
        this.currency = currency;
        this.requested = requested;
        this.available = available;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getCurrency() {
        return currency;
    }

    public BigDecimal getRequested() {
        return requested;
    }

    public BigDecimal getAvailable() {
        return available;
    }
}

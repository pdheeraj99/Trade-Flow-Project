package com.tradeflow.wallet.exception;

import java.util.UUID;

/**
 * Exception thrown when wallet is not found
 */
public class WalletNotFoundException extends RuntimeException {

    private final UUID userId;
    private final String currency;

    public WalletNotFoundException(UUID userId, String currency) {
        super(String.format("Wallet not found for user %s and currency %s", userId, currency));
        this.userId = userId;
        this.currency = currency;
    }

    public WalletNotFoundException(UUID walletId) {
        super(String.format("Wallet not found with ID %s", walletId));
        this.userId = null;
        this.currency = null;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getCurrency() {
        return currency;
    }
}

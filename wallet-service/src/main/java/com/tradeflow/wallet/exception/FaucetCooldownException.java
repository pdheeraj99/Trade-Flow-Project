package com.tradeflow.wallet.exception;

import java.util.UUID;

/**
 * Exception thrown when faucet is used too frequently
 */
public class FaucetCooldownException extends RuntimeException {

    private final UUID userId;
    private final long remainingSeconds;

    public FaucetCooldownException(UUID userId, long remainingSeconds) {
        super(String.format("Faucet cooldown active for user %s. Please wait %d seconds.", userId, remainingSeconds));
        this.userId = userId;
        this.remainingSeconds = remainingSeconds;
    }

    public UUID getUserId() {
        return userId;
    }

    public long getRemainingSeconds() {
        return remainingSeconds;
    }
}

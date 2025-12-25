package com.tradeflow.oms.exception;

/**
 * Exception thrown when client order ID already exists (idempotency check)
 */
public class DuplicateOrderException extends RuntimeException {

    private final String clientOrderId;

    public DuplicateOrderException(String clientOrderId) {
        super("Order with client order ID already exists: " + clientOrderId);
        this.clientOrderId = clientOrderId;
    }

    public String getClientOrderId() {
        return clientOrderId;
    }
}

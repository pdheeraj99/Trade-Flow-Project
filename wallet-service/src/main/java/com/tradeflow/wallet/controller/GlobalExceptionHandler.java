package com.tradeflow.wallet.controller;

import com.tradeflow.wallet.exception.FaucetCooldownException;
import com.tradeflow.wallet.exception.InsufficientFundsException;
import com.tradeflow.wallet.exception.WalletNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.Map;

/**
 * Global exception handler for Wallet Service
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(WalletNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleWalletNotFound(WalletNotFoundException ex) {
        log.warn("Wallet not found: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse("WALLET_NOT_FOUND", ex.getMessage()));
    }

    @ExceptionHandler(InsufficientFundsException.class)
    public ResponseEntity<ErrorResponse> handleInsufficientFunds(InsufficientFundsException ex) {
        log.warn("Insufficient funds: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("INSUFFICIENT_FUNDS", ex.getMessage(), Map.of(
                        "requested", ex.getRequested().toString(),
                        "available", ex.getAvailable().toString(),
                        "currency", ex.getCurrency())));
    }

    @ExceptionHandler(FaucetCooldownException.class)
    public ResponseEntity<ErrorResponse> handleFaucetCooldown(FaucetCooldownException ex) {
        log.info("Faucet cooldown for user {}: {} seconds remaining", ex.getUserId(), ex.getRemainingSeconds());
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .body(new ErrorResponse("FAUCET_COOLDOWN", ex.getMessage(), Map.of(
                        "remainingSeconds", ex.getRemainingSeconds())));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        log.error("Unexpected error", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("INTERNAL_ERROR", "An unexpected error occurred"));
    }

    public record ErrorResponse(String code, String message, Map<String, Object> details, Instant timestamp) {
        public ErrorResponse(String code, String message) {
            this(code, message, null, Instant.now());
        }

        public ErrorResponse(String code, String message, Map<String, Object> details) {
            this(code, message, details, Instant.now());
        }
    }
}

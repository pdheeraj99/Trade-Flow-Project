package com.tradeflow.wallet.controller;

import com.tradeflow.common.dto.WalletBalanceDTO;
import com.tradeflow.wallet.service.WalletService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST Controller for Wallet operations
 */
@RestController
@RequestMapping("/api/wallet")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Wallet", description = "Wallet balance management and faucet operations")
public class WalletController {

    private final WalletService walletService;

    /**
     * Get all balances for a user
     */
    @Operation(summary = "Get all user balances", description = "Retrieve all wallet balances for a user across all currencies")
    @ApiResponse(responseCode = "200", description = "Balances retrieved successfully")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @GetMapping("/balances")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<WalletBalanceDTO>> getBalances(@AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getClaimAsString("userId"));
        log.debug("Getting balances for user {}", userId);
        List<WalletBalanceDTO> balances = walletService.getBalances(userId);
        return ResponseEntity.ok(balances);
    }

    /**
     * Get balance for a specific currency
     */
    @GetMapping("/balance/{currency}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<WalletBalanceDTO> getBalance(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String currency) {
        UUID userId = UUID.fromString(jwt.getClaimAsString("userId"));
        String normalized = validateAndNormalizeCurrency(currency);
        log.debug("Getting {} balance for user {}", normalized, userId);
        WalletBalanceDTO balance = walletService.getBalance(userId, normalized);
        return ResponseEntity.ok(balance);
    }

    /**
     * Claim faucet - adds $10,000 virtual USD
     * Rate limited to once per hour
     */
    @PostMapping("/faucet")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<FaucetResponse> claimFaucet(@AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getClaimAsString("userId"));
        log.info("User {} claiming faucet", userId);
        WalletBalanceDTO balance = walletService.claimFaucet(userId);
        return ResponseEntity.ok(new FaucetResponse(
                "Successfully claimed $10,000 virtual USD!",
                balance));
    }

    /**
     * Create wallet for a currency (usually auto-created on first use)
     */
    @PostMapping("/create/{currency}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<WalletBalanceDTO> createWallet(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String currency) {
        UUID userId = UUID.fromString(jwt.getClaimAsString("userId"));
        String normalized = validateAndNormalizeCurrency(currency);
        log.info("Creating {} wallet for user {}", normalized, userId);
        walletService.getOrCreateWallet(userId, normalized);
        WalletBalanceDTO balance = walletService.getBalance(userId, normalized);
        return ResponseEntity.ok(balance);
    }

    /**
     * Faucet response wrapper
     */
    public record FaucetResponse(String message, WalletBalanceDTO balance) {
    }

    private String validateAndNormalizeCurrency(String currency) {
        if (currency == null) {
            throw new IllegalArgumentException("currency must not be null");
        }
        String normalized = currency.trim().toUpperCase();
        if (!normalized.matches("^[A-Z]{3,10}$")) {
            throw new IllegalArgumentException("currency must be 3-10 uppercase letters");
        }
        return normalized;
    }
}

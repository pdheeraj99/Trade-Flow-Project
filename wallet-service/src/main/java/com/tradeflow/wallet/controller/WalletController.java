package com.tradeflow.wallet.controller;

import com.tradeflow.common.dto.WalletBalanceDTO;
import com.tradeflow.wallet.service.WalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
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
public class WalletController {

    private final WalletService walletService;

    /**
     * Get all balances for a user
     */
    @GetMapping("/{userId}/balances")
    public ResponseEntity<List<WalletBalanceDTO>> getBalances(@PathVariable UUID userId) {
        log.debug("Getting balances for user {}", userId);
        List<WalletBalanceDTO> balances = walletService.getBalances(userId);
        return ResponseEntity.ok(balances);
    }

    /**
     * Get balance for a specific currency
     */
    @GetMapping("/{userId}/balance/{currency}")
    public ResponseEntity<WalletBalanceDTO> getBalance(
            @PathVariable UUID userId,
            @PathVariable String currency) {
        log.debug("Getting {} balance for user {}", currency, userId);
        WalletBalanceDTO balance = walletService.getBalance(userId, currency.toUpperCase());
        return ResponseEntity.ok(balance);
    }

    /**
     * Claim faucet - adds $10,000 virtual USD
     * Rate limited to once per hour
     */
    @PostMapping("/{userId}/faucet")
    public ResponseEntity<FaucetResponse> claimFaucet(@PathVariable UUID userId) {
        log.info("User {} claiming faucet", userId);
        WalletBalanceDTO balance = walletService.claimFaucet(userId);
        return ResponseEntity.ok(new FaucetResponse(
                "Successfully claimed $10,000 virtual USD!",
                balance));
    }

    /**
     * Create wallet for a currency (usually auto-created on first use)
     */
    @PostMapping("/{userId}/create/{currency}")
    public ResponseEntity<WalletBalanceDTO> createWallet(
            @PathVariable UUID userId,
            @PathVariable String currency) {
        log.info("Creating {} wallet for user {}", currency, userId);
        walletService.getOrCreateWallet(userId, currency.toUpperCase());
        WalletBalanceDTO balance = walletService.getBalance(userId, currency.toUpperCase());
        return ResponseEntity.ok(balance);
    }

    /**
     * Faucet response wrapper
     */
    public record FaucetResponse(String message, WalletBalanceDTO balance) {
    }
}

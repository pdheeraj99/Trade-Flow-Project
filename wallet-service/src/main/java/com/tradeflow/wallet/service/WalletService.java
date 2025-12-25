package com.tradeflow.wallet.service;

import com.tradeflow.common.dto.WalletBalanceDTO;
import com.tradeflow.common.enums.TransactionType;
import com.tradeflow.wallet.entity.Wallet;
import com.tradeflow.wallet.entity.WalletBalance;
import com.tradeflow.wallet.entity.WalletTransaction;
import com.tradeflow.wallet.exception.FaucetCooldownException;
import com.tradeflow.wallet.exception.InsufficientFundsException;
import com.tradeflow.wallet.exception.WalletNotFoundException;
import com.tradeflow.wallet.repository.WalletBalanceRepository;
import com.tradeflow.wallet.repository.WalletRepository;
import com.tradeflow.wallet.repository.WalletTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Core Wallet Service implementing double-entry ledger operations.
 * All balance modifications happen through this service.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WalletService {

    private final WalletRepository walletRepository;
    private final WalletBalanceRepository walletBalanceRepository;
    private final WalletTransactionRepository transactionRepository;
    private final StringRedisTemplate redisTemplate;

    private static final String FAUCET_KEY_PREFIX = "faucet:cooldown:";
    private static final Duration FAUCET_COOLDOWN = Duration.ofHours(1);
    private static final BigDecimal FAUCET_AMOUNT = new BigDecimal("10000.00");
    private static final String FAUCET_CURRENCY = "USD";

    /**
     * Get or create a wallet for user and currency
     */
    @Transactional
    public Wallet getOrCreateWallet(UUID userId, String currency) {
        return walletRepository.findByUserIdAndCurrency(userId, currency)
                .orElseGet(() -> createWallet(userId, currency));
    }

    /**
     * Create a new wallet with zero balance
     */
    @Transactional
    public Wallet createWallet(UUID userId, String currency) {
        log.info("Creating wallet for user {} with currency {}", userId, currency);

        Wallet wallet = Wallet.builder()
                .userId(userId)
                .currency(currency)
                .build();
        wallet = walletRepository.save(wallet);

        // Initialize balance record
        WalletBalance balance = WalletBalance.builder()
                .wallet(wallet)
                .availableBalance(BigDecimal.ZERO)
                .reservedBalance(BigDecimal.ZERO)
                .build();
        walletBalanceRepository.save(balance);

        log.info("Created wallet {} for user {}", wallet.getWalletId(), userId);
        return wallet;
    }

    /**
     * Get all balances for a user
     */
    @Transactional(readOnly = true)
    public List<WalletBalanceDTO> getBalances(UUID userId) {
        return walletBalanceRepository.findAllByUserId(userId).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get balance for specific currency
     */
    @Transactional(readOnly = true)
    public WalletBalanceDTO getBalance(UUID userId, String currency) {
        WalletBalance balance = walletBalanceRepository.findByUserIdAndCurrency(userId, currency)
                .orElseThrow(() -> new WalletNotFoundException(userId, currency));
        return toDTO(balance);
    }

    /**
     * Faucet endpoint - adds $10,000 USD to user's wallet
     * Rate limited to once per hour per user via Redis
     */
    @Transactional
    public WalletBalanceDTO claimFaucet(UUID userId) {
        String redisKey = FAUCET_KEY_PREFIX + userId;

        // Check cooldown
        Long ttl = redisTemplate.getExpire(redisKey);
        if (ttl != null && ttl > 0) {
            throw new FaucetCooldownException(userId, ttl);
        }

        // Get or create USD wallet
        Wallet wallet = getOrCreateWallet(userId, FAUCET_CURRENCY);

        // Lock balance for update
        WalletBalance balance = walletBalanceRepository.findByIdForUpdate(wallet.getWalletId())
                .orElseThrow(() -> new WalletNotFoundException(userId, FAUCET_CURRENCY));

        // Create ledger entry
        WalletTransaction transaction = WalletTransaction.builder()
                .wallet(wallet)
                .amount(FAUCET_AMOUNT)
                .referenceType(TransactionType.DEPOSIT)
                .description("Faucet claim - $10,000 virtual USD")
                .build();
        transactionRepository.save(transaction);

        // Update balance
        balance.creditToAvailable(FAUCET_AMOUNT);
        walletBalanceRepository.save(balance);

        // Set cooldown in Redis
        redisTemplate.opsForValue().set(redisKey, "1", FAUCET_COOLDOWN);

        log.info("User {} claimed faucet: {} {}", userId, FAUCET_AMOUNT, FAUCET_CURRENCY);
        return toDTO(balance);
    }

    /**
     * Reserve funds for a pending order (called by Saga)
     * Uses PESSIMISTIC_WRITE lock to prevent race conditions
     */
    @Transactional
    public void reserveFunds(UUID userId, String currency, BigDecimal amount, UUID orderId) {
        log.info("Reserving {} {} for user {} order {}", amount, currency, userId, orderId);

        Wallet wallet = walletRepository.findByUserIdAndCurrency(userId, currency)
                .orElseThrow(() -> new WalletNotFoundException(userId, currency));

        // Lock balance for update
        WalletBalance balance = walletBalanceRepository.findByIdForUpdate(wallet.getWalletId())
                .orElseThrow(() -> new WalletNotFoundException(userId, currency));

        // Check sufficient funds
        if (!balance.hasSufficientBalance(amount)) {
            throw new InsufficientFundsException(userId, currency, amount, balance.getAvailableBalance());
        }

        // Create reserve ledger entry (negative for reserve)
        WalletTransaction transaction = WalletTransaction.builder()
                .wallet(wallet)
                .amount(amount.negate()) // Negative = funds leaving available
                .referenceType(TransactionType.RESERVE)
                .referenceId(orderId)
                .description("Reserve for order " + orderId)
                .build();
        transactionRepository.save(transaction);

        // Update balance atomically
        balance.reserveFunds(amount);
        walletBalanceRepository.save(balance);

        log.info("Reserved {} {} for user {} order {}", amount, currency, userId, orderId);
    }

    /**
     * Release reserved funds back to available (order cancelled/rejected)
     */
    @Transactional
    public void releaseFunds(UUID userId, String currency, BigDecimal amount, UUID orderId) {
        log.info("Releasing {} {} for user {} order {}", amount, currency, userId, orderId);

        Wallet wallet = walletRepository.findByUserIdAndCurrency(userId, currency)
                .orElseThrow(() -> new WalletNotFoundException(userId, currency));

        // Lock balance for update
        WalletBalance balance = walletBalanceRepository.findByIdForUpdate(wallet.getWalletId())
                .orElseThrow(() -> new WalletNotFoundException(userId, currency));

        // Create release ledger entry (positive for release back)
        WalletTransaction transaction = WalletTransaction.builder()
                .wallet(wallet)
                .amount(amount) // Positive = funds returning to available
                .referenceType(TransactionType.RELEASE)
                .referenceId(orderId)
                .description("Release from cancelled order " + orderId)
                .build();
        transactionRepository.save(transaction);

        // Update balance
        balance.releaseFunds(amount);
        walletBalanceRepository.save(balance);

        log.info("Released {} {} for user {} order {}", amount, currency, userId, orderId);
    }

    /**
     * Settle a completed trade - debit from buyer, credit to seller
     */
    @Transactional
    public void settleTrade(UUID buyerId, UUID sellerId, String baseCurrency, String quoteCurrency,
            BigDecimal baseAmount, BigDecimal quoteAmount, UUID tradeId) {
        log.info("Settling trade {}: buyer {} gets {} {}, seller {} gets {} {}",
                tradeId, buyerId, baseAmount, baseCurrency, sellerId, quoteAmount, quoteCurrency);

        // Buyer: debit quote currency from reserved, credit base currency
        settleForBuyer(buyerId, baseCurrency, quoteCurrency, baseAmount, quoteAmount, tradeId);

        // Seller: debit base currency from reserved, credit quote currency
        settleForSeller(sellerId, baseCurrency, quoteCurrency, baseAmount, quoteAmount, tradeId);

        log.info("Trade {} settled successfully", tradeId);
    }

    private void settleForBuyer(UUID buyerId, String baseCurrency, String quoteCurrency,
            BigDecimal baseAmount, BigDecimal quoteAmount, UUID tradeId) {
        // Debit quote currency (e.g., USD) from reserved
        Wallet quoteWallet = walletRepository.findByUserIdAndCurrency(buyerId, quoteCurrency)
                .orElseThrow(() -> new WalletNotFoundException(buyerId, quoteCurrency));
        WalletBalance quoteBalance = walletBalanceRepository.findByIdForUpdate(quoteWallet.getWalletId())
                .orElseThrow(() -> new WalletNotFoundException(buyerId, quoteCurrency));

        transactionRepository.save(WalletTransaction.builder()
                .wallet(quoteWallet)
                .amount(quoteAmount.negate())
                .referenceType(TransactionType.TRADE_DEBIT)
                .referenceId(tradeId)
                .description("Trade debit: Bought " + baseAmount + " " + baseCurrency)
                .build());
        quoteBalance.debitFromReserved(quoteAmount);
        walletBalanceRepository.save(quoteBalance);

        // Credit base currency (e.g., BTC) to available
        Wallet baseWallet = getOrCreateWallet(buyerId, baseCurrency);
        WalletBalance baseBalance = walletBalanceRepository.findByIdForUpdate(baseWallet.getWalletId())
                .orElseThrow(() -> new WalletNotFoundException(buyerId, baseCurrency));

        transactionRepository.save(WalletTransaction.builder()
                .wallet(baseWallet)
                .amount(baseAmount)
                .referenceType(TransactionType.TRADE_CREDIT)
                .referenceId(tradeId)
                .description("Trade credit: Received " + baseAmount + " " + baseCurrency)
                .build());
        baseBalance.creditToAvailable(baseAmount);
        walletBalanceRepository.save(baseBalance);
    }

    private void settleForSeller(UUID sellerId, String baseCurrency, String quoteCurrency,
            BigDecimal baseAmount, BigDecimal quoteAmount, UUID tradeId) {
        // Debit base currency (e.g., BTC) from reserved
        Wallet baseWallet = walletRepository.findByUserIdAndCurrency(sellerId, baseCurrency)
                .orElseThrow(() -> new WalletNotFoundException(sellerId, baseCurrency));
        WalletBalance baseBalance = walletBalanceRepository.findByIdForUpdate(baseWallet.getWalletId())
                .orElseThrow(() -> new WalletNotFoundException(sellerId, baseCurrency));

        transactionRepository.save(WalletTransaction.builder()
                .wallet(baseWallet)
                .amount(baseAmount.negate())
                .referenceType(TransactionType.TRADE_DEBIT)
                .referenceId(tradeId)
                .description("Trade debit: Sold " + baseAmount + " " + baseCurrency)
                .build());
        baseBalance.debitFromReserved(baseAmount);
        walletBalanceRepository.save(baseBalance);

        // Credit quote currency (e.g., USD) to available
        Wallet quoteWallet = getOrCreateWallet(sellerId, quoteCurrency);
        WalletBalance quoteBalance = walletBalanceRepository.findByIdForUpdate(quoteWallet.getWalletId())
                .orElseThrow(() -> new WalletNotFoundException(sellerId, quoteCurrency));

        transactionRepository.save(WalletTransaction.builder()
                .wallet(quoteWallet)
                .amount(quoteAmount)
                .referenceType(TransactionType.TRADE_CREDIT)
                .referenceId(tradeId)
                .description("Trade credit: Received " + quoteAmount + " " + quoteCurrency)
                .build());
        quoteBalance.creditToAvailable(quoteAmount);
        walletBalanceRepository.save(quoteBalance);
    }

    private WalletBalanceDTO toDTO(WalletBalance balance) {
        return WalletBalanceDTO.builder()
                .walletId(balance.getWalletId())
                .userId(balance.getWallet().getUserId())
                .currency(balance.getWallet().getCurrency())
                .availableBalance(balance.getAvailableBalance())
                .reservedBalance(balance.getReservedBalance())
                .build();
    }
}

package com.tradeflow.wallet.service;

import com.tradeflow.common.dto.WalletBalanceDTO;
import com.tradeflow.common.enums.TransactionType;
import com.tradeflow.wallet.config.WalletConfigProperties;
import com.tradeflow.wallet.entity.Wallet;
import com.tradeflow.wallet.entity.WalletBalance;
import com.tradeflow.wallet.entity.WalletTransaction;
import com.tradeflow.wallet.exception.FaucetCooldownException;
import com.tradeflow.wallet.exception.InsufficientFundsException;
import com.tradeflow.wallet.exception.WalletNotFoundException;
import com.tradeflow.wallet.repository.WalletBalanceRepository;
import com.tradeflow.wallet.repository.WalletRepository;
import com.tradeflow.wallet.repository.WalletTransactionRepository;
import com.tradeflow.wallet.util.MoneyUtils;
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
 * 
 * Features:
 * - Double-entry ledger pattern (immutable transactions)
 * - Pessimistic locking for fund reservation
 * - Redis-based idempotency and rate limiting
 * - Configurable via WalletConfigProperties
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WalletService {

        private final WalletRepository walletRepository;
        private final WalletBalanceRepository walletBalanceRepository;
        private final WalletTransactionRepository transactionRepository;
        private final StringRedisTemplate redisTemplate;
        private final WalletConfigProperties config;

        private static final String FAUCET_KEY_PREFIX = "faucet:cooldown:";

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

                // Initialize balance record with proper scale
                WalletBalance balance = WalletBalance.builder()
                                .wallet(wallet)
                                .availableBalance(MoneyUtils.zero(config.getPrecision().getScale()))
                                .reservedBalance(MoneyUtils.zero(config.getPrecision().getScale()))
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
         * Faucet endpoint - adds virtual currency to user's wallet.
         * Amount and cooldown are configurable via application.yml.
         * Rate limited per user via Redis.
         */
        @Transactional
        public WalletBalanceDTO claimFaucet(UUID userId) {
                String redisKey = FAUCET_KEY_PREFIX + userId;

                // Check cooldown
                Long ttl = redisTemplate.getExpire(redisKey);
                if (ttl != null && ttl > 0) {
                        throw new FaucetCooldownException(userId, ttl);
                }

                // Get faucet configuration
                BigDecimal faucetAmount = MoneyUtils.normalize(
                                config.getFaucet().getAmount(),
                                config.getPrecision().getScale());
                String faucetCurrency = config.getFaucet().getCurrency();
                Duration cooldown = Duration.ofSeconds(config.getFaucet().getCooldownSeconds());

                // Get or create wallet for faucet currency
                Wallet wallet = getOrCreateWallet(userId, faucetCurrency);

                // Lock balance for update
                WalletBalance balance = walletBalanceRepository.findByIdForUpdate(wallet.getWalletId())
                                .orElseThrow(() -> new WalletNotFoundException(userId, faucetCurrency));

                // Create ledger entry
                WalletTransaction transaction = WalletTransaction.builder()
                                .wallet(wallet)
                                .amount(faucetAmount)
                                .referenceType(TransactionType.DEPOSIT)
                                .description(String.format("Faucet claim - %s virtual %s", faucetAmount,
                                                faucetCurrency))
                                .build();
                transactionRepository.save(transaction);

                // Update balance with proper scale handling
                balance.creditToAvailable(faucetAmount, config.getPrecision().getScale());
                walletBalanceRepository.save(balance);

                // Set cooldown in Redis
                redisTemplate.opsForValue().set(redisKey, "1", cooldown);

                log.info("User {} claimed faucet: {} {}", userId, faucetAmount, faucetCurrency);
                return toDTO(balance);
        }

        /**
         * Reserve funds for a pending order (called by Saga).
         * Uses PESSIMISTIC_WRITE lock to prevent race conditions.
         */
        @Transactional
        public void reserveFunds(UUID userId, String currency, BigDecimal amount, UUID orderId) {
                BigDecimal normalizedAmount = MoneyUtils.normalize(amount, config.getPrecision().getScale());
                log.info("Reserving {} {} for user {} order {}", normalizedAmount, currency, userId, orderId);

                Wallet wallet = walletRepository.findByUserIdAndCurrency(userId, currency)
                                .orElseThrow(() -> new WalletNotFoundException(userId, currency));

                // Lock balance for update
                WalletBalance balance = walletBalanceRepository.findByIdForUpdate(wallet.getWalletId())
                                .orElseThrow(() -> new WalletNotFoundException(userId, currency));

                // Check sufficient funds
                if (!balance.hasSufficientBalance(normalizedAmount)) {
                        throw new InsufficientFundsException(userId, currency, normalizedAmount,
                                        balance.getAvailableBalance());
                }

                // Create reserve ledger entry (negative for reserve)
                WalletTransaction transaction = WalletTransaction.builder()
                                .wallet(wallet)
                                .amount(MoneyUtils.negate(normalizedAmount, config.getPrecision().getScale()))
                                .referenceType(TransactionType.RESERVE)
                                .referenceId(orderId)
                                .description("Reserve for order " + orderId)
                                .build();
                transactionRepository.save(transaction);

                // Update balance atomically
                balance.reserveFunds(normalizedAmount, config.getPrecision().getScale());
                walletBalanceRepository.save(balance);

                log.info("Reserved {} {} for user {} order {}", normalizedAmount, currency, userId, orderId);
        }

        /**
         * Release reserved funds back to available (order cancelled/rejected)
         */
        @Transactional
        public void releaseFunds(UUID userId, String currency, BigDecimal amount, UUID orderId) {
                BigDecimal normalizedAmount = MoneyUtils.normalize(amount, config.getPrecision().getScale());
                log.info("Releasing {} {} for user {} order {}", normalizedAmount, currency, userId, orderId);

                Wallet wallet = walletRepository.findByUserIdAndCurrency(userId, currency)
                                .orElseThrow(() -> new WalletNotFoundException(userId, currency));

                // Lock balance for update
                WalletBalance balance = walletBalanceRepository.findByIdForUpdate(wallet.getWalletId())
                                .orElseThrow(() -> new WalletNotFoundException(userId, currency));

                // Create release ledger entry (positive for release back)
                WalletTransaction transaction = WalletTransaction.builder()
                                .wallet(wallet)
                                .amount(normalizedAmount)
                                .referenceType(TransactionType.RELEASE)
                                .referenceId(orderId)
                                .description("Release from cancelled order " + orderId)
                                .build();
                transactionRepository.save(transaction);

                // Update balance
                balance.releaseFunds(normalizedAmount, config.getPrecision().getScale());
                walletBalanceRepository.save(balance);

                log.info("Released {} {} for user {} order {}", normalizedAmount, currency, userId, orderId);
        }

        /**
         * Settle a completed trade - debit from buyer, credit to seller
         */
        @Transactional
        public void settleTrade(UUID buyerId, UUID sellerId, String baseCurrency, String quoteCurrency,
                        BigDecimal baseAmount, BigDecimal quoteAmount, UUID tradeId) {
                int scale = config.getPrecision().getScale();
                BigDecimal normalizedBaseAmount = MoneyUtils.normalize(baseAmount, scale);
                BigDecimal normalizedQuoteAmount = MoneyUtils.normalize(quoteAmount, scale);

                log.info("Settling trade {}: buyer {} gets {} {}, seller {} gets {} {}",
                                tradeId, buyerId, normalizedBaseAmount, baseCurrency, sellerId, normalizedQuoteAmount,
                                quoteCurrency);

                // Buyer: debit quote currency from reserved, credit base currency
                settleForBuyer(buyerId, baseCurrency, quoteCurrency, normalizedBaseAmount, normalizedQuoteAmount,
                                tradeId);

                // Seller: debit base currency from reserved, credit quote currency
                settleForSeller(sellerId, baseCurrency, quoteCurrency, normalizedBaseAmount, normalizedQuoteAmount,
                                tradeId);

                log.info("Trade {} settled successfully", tradeId);
        }

        private void settleForBuyer(UUID buyerId, String baseCurrency, String quoteCurrency,
                        BigDecimal baseAmount, BigDecimal quoteAmount, UUID tradeId) {
                int scale = config.getPrecision().getScale();

                // Debit quote currency (e.g., USD) from reserved
                Wallet quoteWallet = walletRepository.findByUserIdAndCurrency(buyerId, quoteCurrency)
                                .orElseThrow(() -> new WalletNotFoundException(buyerId, quoteCurrency));
                WalletBalance quoteBalance = walletBalanceRepository.findByIdForUpdate(quoteWallet.getWalletId())
                                .orElseThrow(() -> new WalletNotFoundException(buyerId, quoteCurrency));

                transactionRepository.save(WalletTransaction.builder()
                                .wallet(quoteWallet)
                                .amount(MoneyUtils.negate(quoteAmount, scale))
                                .referenceType(TransactionType.TRADE_DEBIT)
                                .referenceId(tradeId)
                                .description("Trade debit: Bought " + baseAmount + " " + baseCurrency)
                                .build());
                quoteBalance.debitFromReserved(quoteAmount, scale);
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
                baseBalance.creditToAvailable(baseAmount, scale);
                walletBalanceRepository.save(baseBalance);
        }

        private void settleForSeller(UUID sellerId, String baseCurrency, String quoteCurrency,
                        BigDecimal baseAmount, BigDecimal quoteAmount, UUID tradeId) {
                int scale = config.getPrecision().getScale();

                // Debit base currency (e.g., BTC) from reserved
                Wallet baseWallet = walletRepository.findByUserIdAndCurrency(sellerId, baseCurrency)
                                .orElseThrow(() -> new WalletNotFoundException(sellerId, baseCurrency));
                WalletBalance baseBalance = walletBalanceRepository.findByIdForUpdate(baseWallet.getWalletId())
                                .orElseThrow(() -> new WalletNotFoundException(sellerId, baseCurrency));

                transactionRepository.save(WalletTransaction.builder()
                                .wallet(baseWallet)
                                .amount(MoneyUtils.negate(baseAmount, scale))
                                .referenceType(TransactionType.TRADE_DEBIT)
                                .referenceId(tradeId)
                                .description("Trade debit: Sold " + baseAmount + " " + baseCurrency)
                                .build());
                baseBalance.debitFromReserved(baseAmount, scale);
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
                quoteBalance.creditToAvailable(quoteAmount, scale);
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

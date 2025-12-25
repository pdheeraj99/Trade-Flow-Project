package com.tradeflow.wallet.repository;

import com.tradeflow.wallet.entity.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Wallet entity operations
 */
@Repository
public interface WalletRepository extends JpaRepository<Wallet, UUID> {

    /**
     * Find wallet by user ID and currency
     */
    Optional<Wallet> findByUserIdAndCurrency(UUID userId, String currency);

    /**
     * Find all wallets for a user
     */
    List<Wallet> findByUserId(UUID userId);

    /**
     * Check if wallet exists for user and currency
     */
    boolean existsByUserIdAndCurrency(UUID userId, String currency);
}

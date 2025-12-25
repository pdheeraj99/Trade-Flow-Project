package com.tradeflow.wallet.repository;

import com.tradeflow.common.enums.TransactionType;
import com.tradeflow.wallet.entity.WalletTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Repository for WalletTransaction (immutable ledger entries)
 */
@Repository
public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, UUID> {

    /**
     * Find all transactions for a wallet
     */
    List<WalletTransaction> findByWalletWalletIdOrderByCreatedAtDesc(UUID walletId);

    /**
     * Find transactions by reference (e.g., order ID)
     */
    List<WalletTransaction> findByReferenceTypeAndReferenceId(TransactionType type, UUID referenceId);

    /**
     * Paginated transactions for a wallet
     */
    Page<WalletTransaction> findByWalletWalletId(UUID walletId, Pageable pageable);

    /**
     * Calculate sum of all transactions for a wallet (for balance verification)
     */
    @Query("SELECT COALESCE(SUM(wt.amount), 0) FROM WalletTransaction wt WHERE wt.wallet.walletId = :walletId")
    BigDecimal calculateTotalBalance(@Param("walletId") UUID walletId);

    /**
     * Find transactions for a user across all wallets
     */
    @Query("SELECT wt FROM WalletTransaction wt JOIN wt.wallet w WHERE w.userId = :userId ORDER BY wt.createdAt DESC")
    Page<WalletTransaction> findByUserId(@Param("userId") UUID userId, Pageable pageable);
}

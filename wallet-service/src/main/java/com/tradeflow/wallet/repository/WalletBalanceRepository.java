package com.tradeflow.wallet.repository;

import com.tradeflow.wallet.entity.WalletBalance;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for WalletBalance entity with pessimistic locking support
 */
@Repository
public interface WalletBalanceRepository extends JpaRepository<WalletBalance, UUID> {

    /**
     * Find balance by wallet ID with PESSIMISTIC_WRITE lock.
     * This prevents concurrent modifications during fund reservation.
     * Issues: SELECT ... FOR UPDATE
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT wb FROM WalletBalance wb WHERE wb.walletId = :walletId")
    Optional<WalletBalance> findByIdForUpdate(@Param("walletId") UUID walletId);

    /**
     * Find balance by user ID and currency with PESSIMISTIC_WRITE lock.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT wb FROM WalletBalance wb JOIN wb.wallet w WHERE w.userId = :userId AND w.currency = :currency")
    Optional<WalletBalance> findByUserIdAndCurrencyForUpdate(@Param("userId") UUID userId,
            @Param("currency") String currency);

    /**
     * Find all balances for a user (read-only, no lock)
     */
    @Query("SELECT wb FROM WalletBalance wb JOIN FETCH wb.wallet w WHERE w.userId = :userId")
    List<WalletBalance> findAllByUserId(@Param("userId") UUID userId);

    /**
     * Find balance by user ID and currency (read-only)
     */
    @Query("SELECT wb FROM WalletBalance wb JOIN FETCH wb.wallet w WHERE w.userId = :userId AND w.currency = :currency")
    Optional<WalletBalance> findByUserIdAndCurrency(@Param("userId") UUID userId, @Param("currency") String currency);
}

package com.tradeflow.wallet.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * WalletBalance entity - Materialized view of wallet balances.
 * Updated atomically within the same transaction as ledger inserts.
 * This avoids expensive SUM queries on the transaction table.
 */
@Entity
@Table(name = "wallet_balances", schema = "wallet")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WalletBalance {

    @Id
    @Column(name = "wallet_id", updatable = false, nullable = false)
    private UUID walletId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "wallet_id")
    private Wallet wallet;

    /**
     * Available balance - can be used for new orders
     */
    @Column(name = "available_balance", nullable = false, precision = 20, scale = 8)
    @Builder.Default
    private BigDecimal availableBalance = BigDecimal.ZERO;

    /**
     * Reserved balance - locked for pending orders
     */
    @Column(name = "reserved_balance", nullable = false, precision = 20, scale = 8)
    @Builder.Default
    private BigDecimal reservedBalance = BigDecimal.ZERO;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(name = "version")
    private Long version;

    /**
     * Total balance = available + reserved
     */
    public BigDecimal getTotalBalance() {
        return availableBalance.add(reservedBalance);
    }

    /**
     * Check if sufficient available balance exists
     */
    public boolean hasSufficientBalance(BigDecimal amount) {
        return availableBalance.compareTo(amount) >= 0;
    }

    /**
     * Reserve funds from available balance
     */
    public void reserveFunds(BigDecimal amount) {
        if (!hasSufficientBalance(amount)) {
            throw new IllegalStateException("Insufficient available balance");
        }
        this.availableBalance = this.availableBalance.subtract(amount);
        this.reservedBalance = this.reservedBalance.add(amount);
    }

    /**
     * Release reserved funds back to available
     */
    public void releaseFunds(BigDecimal amount) {
        if (this.reservedBalance.compareTo(amount) < 0) {
            throw new IllegalStateException("Cannot release more than reserved");
        }
        this.reservedBalance = this.reservedBalance.subtract(amount);
        this.availableBalance = this.availableBalance.add(amount);
    }

    /**
     * Debit from reserved (for completed trades)
     */
    public void debitFromReserved(BigDecimal amount) {
        if (this.reservedBalance.compareTo(amount) < 0) {
            throw new IllegalStateException("Cannot debit more than reserved");
        }
        this.reservedBalance = this.reservedBalance.subtract(amount);
    }

    /**
     * Credit to available (for deposits or trade proceeds)
     */
    public void creditToAvailable(BigDecimal amount) {
        this.availableBalance = this.availableBalance.add(amount);
    }
}

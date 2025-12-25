package com.tradeflow.wallet.entity;

import com.tradeflow.wallet.util.MoneyUtils;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.math.RoundingMode;
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
        return MoneyUtils.add(availableBalance, reservedBalance);
    }

    /**
     * Check if sufficient available balance exists
     */
    public boolean hasSufficientBalance(BigDecimal amount) {
        return availableBalance.compareTo(amount) >= 0;
    }

    /**
     * Reserve funds from available balance (with scale)
     */
    public void reserveFunds(BigDecimal amount, int scale) {
        if (!hasSufficientBalance(amount)) {
            throw new IllegalStateException("Insufficient available balance");
        }
        this.availableBalance = this.availableBalance.subtract(amount)
                .setScale(scale, RoundingMode.HALF_UP);
        this.reservedBalance = this.reservedBalance.add(amount)
                .setScale(scale, RoundingMode.HALF_UP);
    }

    /**
     * Reserve funds from available balance (default scale)
     */
    public void reserveFunds(BigDecimal amount) {
        reserveFunds(amount, MoneyUtils.DEFAULT_SCALE);
    }

    /**
     * Release reserved funds back to available (with scale)
     */
    public void releaseFunds(BigDecimal amount, int scale) {
        if (this.reservedBalance.compareTo(amount) < 0) {
            throw new IllegalStateException("Cannot release more than reserved");
        }
        this.reservedBalance = this.reservedBalance.subtract(amount)
                .setScale(scale, RoundingMode.HALF_UP);
        this.availableBalance = this.availableBalance.add(amount)
                .setScale(scale, RoundingMode.HALF_UP);
    }

    /**
     * Release reserved funds back to available (default scale)
     */
    public void releaseFunds(BigDecimal amount) {
        releaseFunds(amount, MoneyUtils.DEFAULT_SCALE);
    }

    /**
     * Debit from reserved (for completed trades) with scale
     */
    public void debitFromReserved(BigDecimal amount, int scale) {
        if (this.reservedBalance.compareTo(amount) < 0) {
            throw new IllegalStateException("Cannot debit more than reserved");
        }
        this.reservedBalance = this.reservedBalance.subtract(amount)
                .setScale(scale, RoundingMode.HALF_UP);
    }

    /**
     * Debit from reserved (for completed trades) default scale
     */
    public void debitFromReserved(BigDecimal amount) {
        debitFromReserved(amount, MoneyUtils.DEFAULT_SCALE);
    }

    /**
     * Credit to available (for deposits or trade proceeds) with scale
     */
    public void creditToAvailable(BigDecimal amount, int scale) {
        this.availableBalance = this.availableBalance.add(amount)
                .setScale(scale, RoundingMode.HALF_UP);
    }

    /**
     * Credit to available (for deposits or trade proceeds) default scale
     */
    public void creditToAvailable(BigDecimal amount) {
        creditToAvailable(amount, MoneyUtils.DEFAULT_SCALE);
    }
}

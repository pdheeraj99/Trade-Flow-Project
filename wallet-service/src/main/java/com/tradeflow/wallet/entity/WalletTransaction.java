package com.tradeflow.wallet.entity;

import com.tradeflow.common.enums.TransactionType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * WalletTransaction entity implementing double-entry ledger pattern.
 * Every transaction is immutable - we never update, only insert.
 * Positive amount = credit, Negative amount = debit.
 */
@Entity
@Table(name = "wallet_transactions", schema = "wallet", indexes = {
        @Index(name = "idx_wallet_transactions_wallet_id", columnList = "wallet_id"),
        @Index(name = "idx_wallet_transactions_reference", columnList = "reference_type, reference_id"),
        @Index(name = "idx_wallet_transactions_created_at", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WalletTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "transaction_id", updatable = false, nullable = false)
    private UUID transactionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wallet_id", nullable = false)
    private Wallet wallet;

    /**
     * Amount with 8 decimal precision for cryptocurrency support.
     * Positive = credit (money in), Negative = debit (money out)
     */
    @Column(name = "amount", nullable = false, precision = 20, scale = 8)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "reference_type", nullable = false, length = 20)
    private TransactionType referenceType;

    /**
     * Reference to the business entity (Order ID, Transfer ID, etc.)
     */
    @Column(name = "reference_id")
    private UUID referenceId;

    /**
     * Description for audit purposes
     */
    @Column(name = "description", length = 255)
    private String description;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}

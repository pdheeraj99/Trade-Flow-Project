package com.tradeflow.wallet.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * Wallet entity representing a user's currency account.
 * Each user can have multiple wallets (one per currency: USD, BTC, ETH, etc.)
 */
@Entity
@Table(name = "wallets", schema = "wallet", uniqueConstraints = @UniqueConstraint(columnNames = { "user_id",
        "currency" }))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Wallet {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "wallet_id", updatable = false, nullable = false)
    private UUID walletId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "currency", nullable = false, length = 10)
    private String currency;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Version
    @Column(name = "version")
    private Long version;
}

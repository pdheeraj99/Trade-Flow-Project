package com.tradeflow.common.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Wallet Balance Data Transfer Object
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WalletBalanceDTO {

    private UUID walletId;

    private UUID userId;

    private String currency;

    private BigDecimal availableBalance;

    private BigDecimal reservedBalance;

    /**
     * Total balance = available + reserved
     * JsonProperty ensures this computed field is included in JSON serialization
     */
    @JsonProperty("totalBalance")
    public BigDecimal getTotalBalance() {
        BigDecimal available = availableBalance != null ? availableBalance : BigDecimal.ZERO;
        BigDecimal reserved = reservedBalance != null ? reservedBalance : BigDecimal.ZERO;
        return available.add(reserved);
    }
}

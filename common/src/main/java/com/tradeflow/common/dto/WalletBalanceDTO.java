package com.tradeflow.common.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "Wallet balance information with available and reserved funds")
public class WalletBalanceDTO {

    @Schema(description = "Wallet ID", example = "123e4567-e89b-12d3-a456-426614174000")
    private UUID walletId;

    @Schema(description = "User ID", example = "123e4567-e89b-12d3-a456-426614174000")
    private UUID userId;

    @Schema(description = "Currency code", example = "USD", allowableValues = {"USD", "BTC", "ETH"})
    private String currency;

    @Schema(description = "Available balance for trading", type = "string", example = "10000.50000000")
    @JsonSerialize(using = ToStringSerializer.class)
    private BigDecimal availableBalance;

    @Schema(description = "Reserved balance for pending orders", type = "string", example = "500.25000000")
    @JsonSerialize(using = ToStringSerializer.class)
    private BigDecimal reservedBalance;

    /**
     * Total balance = available + reserved
     * JsonProperty ensures this computed field is included in JSON serialization
     */
    @Schema(description = "Total balance (available + reserved)", type = "string", example = "10500.75000000")
    @JsonProperty("totalBalance")
    @JsonSerialize(using = ToStringSerializer.class)
    public BigDecimal getTotalBalance() {
        BigDecimal available = availableBalance != null ? availableBalance : BigDecimal.ZERO;
        BigDecimal reserved = reservedBalance != null ? reservedBalance : BigDecimal.ZERO;
        return available.add(reserved);
    }
}

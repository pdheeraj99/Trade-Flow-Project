package com.tradeflow.common.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Command sent to Wallet Service to settle a completed trade
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SettleTradeCommand {

    private UUID commandId;

    private UUID sagaId;

    private UUID tradeId;

    private String symbol;

    // Buyer details
    private UUID buyerId;
    private String buyerDebitCurrency; // e.g., USDT
    private String buyerDebitAmount;
    private String buyerCreditCurrency; // e.g., BTC
    private String buyerCreditAmount;

    // Seller details
    private UUID sellerId;
    private String sellerDebitCurrency; // e.g., BTC
    private String sellerDebitAmount;
    private String sellerCreditCurrency; // e.g., USDT
    private String sellerCreditAmount;

    private Instant commandTimestamp;
}

package com.tradeflow.common.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Event published when funds are successfully reserved by Wallet Service
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FundsReservedEvent {

    private UUID eventId;

    private UUID sagaId;

    private UUID orderId;

    private UUID userId;

    private UUID walletId;

    private String currency;

    private String amount;

    private String transactionId;

    private Instant eventTimestamp;
}

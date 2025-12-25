package com.tradeflow.common.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Command sent from OMS to Wallet Service to release reserved funds
 * (compensation)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReleaseFundsCommand {

    private UUID commandId;

    private UUID sagaId;

    private UUID orderId;

    private UUID userId;

    private String currency;

    private String amount;

    private String reason; // e.g., "ORDER_CANCELLED", "ORDER_REJECTED"

    private Instant commandTimestamp;
}

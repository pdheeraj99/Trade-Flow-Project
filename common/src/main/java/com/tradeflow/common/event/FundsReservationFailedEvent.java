package com.tradeflow.common.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Event published when fund reservation fails
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FundsReservationFailedEvent {

    private UUID eventId;

    private UUID sagaId;

    private UUID orderId;

    private UUID userId;

    private String currency;

    private String requestedAmount;

    private String availableBalance;

    private String reason;

    private Instant eventTimestamp;
}

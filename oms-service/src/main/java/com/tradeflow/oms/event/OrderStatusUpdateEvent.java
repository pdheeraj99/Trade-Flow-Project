package com.tradeflow.oms.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderStatusUpdateEvent {
    private UUID orderId;
    private UUID userId;
    private String symbol;
    private String status;
    private double filledQuantity;
    private Instant timestamp;
}

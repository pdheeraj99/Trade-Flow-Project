package com.tradeflow.common.event;

import com.tradeflow.common.dto.OrderBookDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Event published when the order book changes
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderBookUpdateEvent {

    private UUID eventId;

    private String symbol;

    private OrderBookDTO orderBook;

    private long updateId;

    private Instant eventTimestamp;
}

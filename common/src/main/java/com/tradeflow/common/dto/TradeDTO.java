package com.tradeflow.common.dto;

import com.tradeflow.common.enums.OrderSide;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Trade Execution Data Transfer Object
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TradeDTO {

    private UUID tradeId;

    private String symbol;

    private UUID buyOrderId;

    private UUID sellOrderId;

    private UUID buyerId;

    private UUID sellerId;

    private BigDecimal price;

    private BigDecimal quantity;

    private BigDecimal quoteAmount; // price * quantity

    private OrderSide takerSide; // Which side was the taker (aggressor)

    private Instant executedAt;
}

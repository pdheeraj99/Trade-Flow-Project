package com.tradeflow.oms.saga;

import com.tradeflow.common.enums.OrderSide;
import com.tradeflow.common.enums.OrderType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Context object holding all data needed for the order saga.
 * Serialized as JSON and stored in SagaInstance.payload
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderSagaContext {

    private UUID orderId;
    private UUID userId;
    private String symbol;
    private OrderSide side;
    private OrderType type;
    private BigDecimal price;
    private BigDecimal quantity;

    // Derived fields
    private String baseCurrency; // e.g., BTC from BTCUSDT
    private String quoteCurrency; // e.g., USDT from BTCUSDT

    // Calculated amounts
    private BigDecimal reserveAmount; // Amount to reserve in wallet
    private String reserveCurrency; // Currency to reserve

    // Saga state tracking
    private String walletTransactionId; // Transaction ID after fund reservation
    private boolean fundsReserved;
    private boolean orderSentToMatching;
}

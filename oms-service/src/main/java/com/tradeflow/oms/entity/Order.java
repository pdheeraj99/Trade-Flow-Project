package com.tradeflow.oms.entity;

import com.tradeflow.common.enums.OrderSide;
import com.tradeflow.common.enums.OrderStatus;
import com.tradeflow.common.enums.OrderType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Order entity representing a trading order.
 * Tracks the full lifecycle from submission to completion/cancellation.
 */
@Entity
@Table(name = "orders", schema = "orders", indexes = {
        @Index(name = "idx_orders_user_id", columnList = "user_id"),
        @Index(name = "idx_orders_symbol", columnList = "symbol"),
        @Index(name = "idx_orders_status", columnList = "status"),
        @Index(name = "idx_orders_created_at", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "order_id", updatable = false, nullable = false)
    private UUID orderId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "symbol", nullable = false, length = 20)
    private String symbol;

    @Enumerated(EnumType.STRING)
    @Column(name = "side", nullable = false, length = 10)
    private OrderSide side;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 10)
    private OrderType type;

    /**
     * Limit price (null for market orders)
     */
    @Column(name = "price", precision = 20, scale = 8)
    private BigDecimal price;

    /**
     * Original order quantity
     */
    @Column(name = "quantity", nullable = false, precision = 20, scale = 8)
    private BigDecimal quantity;

    /**
     * Quantity that has been filled
     */
    @Column(name = "filled_quantity", nullable = false, precision = 20, scale = 8)
    @Builder.Default
    private BigDecimal filledQuantity = BigDecimal.ZERO;

    /**
     * Amount reserved in quote currency (for buy orders)
     * or base currency (for sell orders)
     */
    @Column(name = "reserved_amount", precision = 20, scale = 8)
    private BigDecimal reservedAmount;

    /**
     * Average fill price
     */
    @Column(name = "avg_fill_price", precision = 20, scale = 8)
    private BigDecimal avgFillPrice;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private OrderStatus status = OrderStatus.PENDING_VALIDATION;

    /**
     * Client-provided order ID for idempotency
     */
    @Column(name = "client_order_id", length = 50)
    private String clientOrderId;

    /**
     * Reason for rejection/cancellation
     */
    @Column(name = "reject_reason", length = 255)
    private String rejectReason;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "filled_at")
    private Instant filledAt;

    @Version
    @Column(name = "version")
    private Long version;

    /**
     * Calculate remaining quantity
     */
    public BigDecimal getRemainingQuantity() {
        return quantity.subtract(filledQuantity);
    }

    /**
     * Check if order is fully filled
     */
    public boolean isFullyFilled() {
        return filledQuantity.compareTo(quantity) >= 0;
    }

    /**
     * Check if order is partially filled
     */
    public boolean isPartiallyFilled() {
        return filledQuantity.compareTo(BigDecimal.ZERO) > 0 &&
                filledQuantity.compareTo(quantity) < 0;
    }

    /**
     * Check if order can be cancelled
     */
    public boolean isCancellable() {
        return status == OrderStatus.OPEN ||
                status == OrderStatus.PARTIALLY_FILLED ||
                status == OrderStatus.PENDING_VALIDATION ||
                status == OrderStatus.FUNDS_RESERVED;
    }

    /**
     * Check if order is in a terminal state
     */
    public boolean isTerminal() {
        return status == OrderStatus.FILLED ||
                status == OrderStatus.CANCELLED ||
                status == OrderStatus.REJECTED ||
                status == OrderStatus.EXPIRED;
    }
}

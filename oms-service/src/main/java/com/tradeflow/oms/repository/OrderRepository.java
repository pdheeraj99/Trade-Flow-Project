package com.tradeflow.oms.repository;

import com.tradeflow.common.enums.OrderStatus;
import com.tradeflow.oms.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Order entity operations
 */
@Repository
public interface OrderRepository extends JpaRepository<Order, UUID> {

    /**
     * Find orders by user ID
     */
    Page<Order> findByUserId(UUID userId, Pageable pageable);

    /**
     * Find orders by user ID and status
     */
    List<Order> findByUserIdAndStatus(UUID userId, OrderStatus status);

    /**
     * Find open orders by user and symbol
     */
    @Query("SELECT o FROM Order o WHERE o.userId = :userId AND o.symbol = :symbol " +
            "AND o.status IN ('OPEN', 'PARTIALLY_FILLED')")
    List<Order> findOpenOrdersByUserAndSymbol(@Param("userId") UUID userId, @Param("symbol") String symbol);

    /**
     * Find all open orders for a symbol
     */
    @Query("SELECT o FROM Order o WHERE o.symbol = :symbol " +
            "AND o.status IN ('OPEN', 'PARTIALLY_FILLED') ORDER BY o.createdAt ASC")
    List<Order> findOpenOrdersBySymbol(@Param("symbol") String symbol);

    /**
     * Find order by client order ID (for idempotency)
     */
    Optional<Order> findByClientOrderId(String clientOrderId);

    /**
     * Check if client order ID already exists
     */
    boolean existsByClientOrderId(String clientOrderId);

    /**
     * Find orders pending to be sent to matching engine
     */
    @Query("SELECT o FROM Order o WHERE o.status = 'FUNDS_RESERVED' ORDER BY o.createdAt ASC")
    List<Order> findOrdersPendingMatching();

    /**
     * Count active orders by user
     */
    @Query("SELECT COUNT(o) FROM Order o WHERE o.userId = :userId " +
            "AND o.status IN ('PENDING_VALIDATION', 'FUNDS_RESERVED', 'OPEN', 'PARTIALLY_FILLED')")
    long countActiveOrdersByUser(@Param("userId") UUID userId);
}

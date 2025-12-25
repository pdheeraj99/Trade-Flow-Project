package com.tradeflow.oms.repository;

import com.tradeflow.common.enums.SagaState;
import com.tradeflow.oms.entity.SagaInstance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for SagaInstance entity operations
 */
@Repository
public interface SagaInstanceRepository extends JpaRepository<SagaInstance, UUID> {

    /**
     * Find saga by order ID
     */
    Optional<SagaInstance> findByOrderOrderId(UUID orderId);

    /**
     * Find sagas by state
     */
    List<SagaInstance> findByState(SagaState state);

    /**
     * Find stale sagas that need recovery
     * (in progress but not updated for a while)
     */
    @Query("SELECT s FROM SagaInstance s WHERE s.state IN ('STARTED', 'FUNDS_RESERVED', 'ORDER_SENT', 'COMPENSATING') "
            +
            "AND s.lastProcessedAt < :threshold AND s.retryCount < s.maxRetries")
    List<SagaInstance> findStaleSagas(@Param("threshold") Instant threshold);

    /**
     * Find failed sagas that can be retried
     */
    @Query("SELECT s FROM SagaInstance s WHERE s.state = 'FAILED' " +
            "AND s.retryCount < s.maxRetries")
    List<SagaInstance> findRetryableSagas();

    /**
     * Count active sagas (not in terminal state)
     */
    @Query("SELECT COUNT(s) FROM SagaInstance s WHERE s.state NOT IN ('COMPLETED', 'FAILED', 'COMPENSATED')")
    long countActiveSagas();
}

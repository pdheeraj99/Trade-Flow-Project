package com.tradeflow.oms.entity;

import com.tradeflow.common.enums.SagaState;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * SagaInstance entity for tracking distributed transaction state.
 * Used by the Saga Orchestrator to manage order workflow.
 */
@Entity
@Table(name = "saga_instances", schema = "orders", indexes = {
        @Index(name = "idx_saga_order_id", columnList = "order_id", unique = true),
        @Index(name = "idx_saga_state", columnList = "state"),
        @Index(name = "idx_saga_created_at", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SagaInstance {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "saga_id", updatable = false, nullable = false)
    private UUID sagaId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Enumerated(EnumType.STRING)
    @Column(name = "state", nullable = false, length = 30)
    @Builder.Default
    private SagaState state = SagaState.STARTED;

    /**
     * Current step in the saga (for tracking progress)
     */
    @Column(name = "current_step", length = 50)
    private String currentStep;

    /**
     * Payload data as JSON for saga context
     */
    @Column(name = "payload", columnDefinition = "TEXT")
    private String payload;

    /**
     * Error message if saga failed
     */
    @Column(name = "error_message", length = 500)
    private String errorMessage;

    /**
     * Number of retry attempts
     */
    @Column(name = "retry_count", nullable = false)
    @Builder.Default
    private int retryCount = 0;

    /**
     * Maximum retries allowed
     */
    @Column(name = "max_retries", nullable = false)
    @Builder.Default
    private int maxRetries = 3;

    /**
     * When saga was last processed
     */
    @Column(name = "last_processed_at")
    private Instant lastProcessedAt;

    /**
     * When saga completed (success or failure)
     */
    @Column(name = "completed_at")
    private Instant completedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(name = "version")
    private Long version;

    /**
     * Move saga to next state
     */
    public void transitionTo(SagaState newState) {
        this.state = newState;
        this.lastProcessedAt = Instant.now();
    }

    /**
     * Mark saga as completed
     */
    public void complete() {
        this.state = SagaState.COMPLETED;
        this.completedAt = Instant.now();
        this.lastProcessedAt = Instant.now();
    }

    /**
     * Mark saga as failed
     */
    public void fail(String errorMessage) {
        this.state = SagaState.FAILED;
        this.errorMessage = errorMessage;
        this.completedAt = Instant.now();
        this.lastProcessedAt = Instant.now();
    }

    /**
     * Check if retry is allowed
     */
    public boolean canRetry() {
        return retryCount < maxRetries;
    }

    /**
     * Increment retry count
     */
    public void incrementRetry() {
        this.retryCount++;
        this.lastProcessedAt = Instant.now();
    }

    /**
     * Check if saga is in a terminal state
     */
    public boolean isTerminal() {
        return state == SagaState.COMPLETED ||
                state == SagaState.FAILED ||
                state == SagaState.COMPENSATED;
    }
}

package com.tradeflow.audit.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * Mongo document capturing audit events across services.
 */
@Document(collection = "audit_logs")
@CompoundIndex(def = "{'eventType': 1, 'timestamp': -1}")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLog {
    @Id
    private String id;
    private String eventType;
    private String payload;
    private Instant timestamp;
    @Indexed
    private String userId;
    @Indexed
    private String correlationId;
}

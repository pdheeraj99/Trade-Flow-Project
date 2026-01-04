package com.tradeflow.audit.messaging;

import com.tradeflow.audit.model.AuditLog;
import com.tradeflow.audit.repository.AuditLogRepository;
import com.tradeflow.common.constants.RabbitMQConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Listens to order exchange and persists audit events.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class GlobalAuditListener {

    private final AuditLogRepository auditLogRepository;

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "audit.events", durable = "true"),
            exchange = @Exchange(value = RabbitMQConstants.ORDER_EXCHANGE, type = "direct", durable = "true"),
            key = "#"))
    public void handle(String payload, @Header(name = "event-type", required = false) String eventType) {
        AuditLog logEntry = AuditLog.builder()
                .eventType(eventType != null ? eventType : "unknown")
                .payload(payload)
                .timestamp(Instant.now())
                .build();
        auditLogRepository.save(logEntry);
        log.info("[AUDIT] persisted eventType={} payload={}", logEntry.getEventType(), payload);
    }
}

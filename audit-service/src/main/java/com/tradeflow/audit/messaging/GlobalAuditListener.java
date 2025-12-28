package com.tradeflow.audit.messaging;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Wildcard audit listener to log all events published to trade-exchange.
 */
@Component
@Slf4j
public class GlobalAuditListener {

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "audit.wildcard.queue", durable = "true"),
            exchange = @Exchange(value = "trade-exchange", type = "topic", durable = "true"),
            key = "#"))
    public void handle(Object payload) {
        log.info("[AUDIT] {}", payload);
    }
}

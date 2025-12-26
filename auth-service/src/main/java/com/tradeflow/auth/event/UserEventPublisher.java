package com.tradeflow.auth.event;

import com.tradeflow.common.constants.RabbitMQConstants;
import com.tradeflow.common.event.UserCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

/**
 * Publisher for user lifecycle events.
 * Sends events to wallet-service via RabbitMQ.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class UserEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    /**
     * Publish UserCreatedEvent when a new user registers.
     * Wallet-service listens to this event to auto-create USD wallet.
     */
    public void publishUserCreated(UUID userId, String username, String email) {
        UserCreatedEvent event = UserCreatedEvent.builder()
                .userId(userId)
                .username(username)
                .email(email)
                .createdAt(Instant.now())
                .build();

        log.info("Publishing UserCreatedEvent for user: {} (ID: {})", username, userId);

        rabbitTemplate.convertAndSend(
                RabbitMQConstants.USER_EXCHANGE,
                RabbitMQConstants.ROUTING_USER_CREATED,
                event);

        log.debug("UserCreatedEvent published successfully");
    }
}

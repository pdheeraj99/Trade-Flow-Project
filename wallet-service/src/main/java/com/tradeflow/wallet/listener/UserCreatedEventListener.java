package com.tradeflow.wallet.listener;

import com.tradeflow.common.constants.RabbitMQConstants;
import com.tradeflow.common.event.UserCreatedEvent;
import com.tradeflow.wallet.service.WalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Listens for UserCreatedEvent from auth-service.
 * Auto-creates USD wallet for new users.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class UserCreatedEventListener {

    private final WalletService walletService;

    /**
     * Handle user creation event.
     * Creates a default USD wallet for the new user.
     */
    @RabbitListener(queues = RabbitMQConstants.USER_CREATED_QUEUE)
    public void handleUserCreatedEvent(UserCreatedEvent event) {
        log.info("Received UserCreatedEvent for user: {} (ID: {})",
                event.getUsername(), event.getUserId());

        try {
            // Create default USD wallet for new user
            walletService.getOrCreateWallet(event.getUserId(), "USD");
            log.info("Successfully created USD wallet for user: {}", event.getUserId());

            // Optionally create BTC wallet as well for trading
            walletService.getOrCreateWallet(event.getUserId(), "BTC");
            log.info("Successfully created BTC wallet for user: {}", event.getUserId());

        } catch (Exception e) {
            log.error("Failed to create wallet for user: {}. Error: {}",
                    event.getUserId(), e.getMessage(), e);
            // In production, you might want to send this to DLQ for retry
            throw e;
        }
    }
}

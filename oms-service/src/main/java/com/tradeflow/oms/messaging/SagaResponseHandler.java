package com.tradeflow.oms.messaging;

import com.rabbitmq.client.Channel;
import com.tradeflow.common.constants.RabbitMQConstants;
import com.tradeflow.common.event.FundsReservationFailedEvent;
import com.tradeflow.common.event.FundsReservedEvent;
import com.tradeflow.oms.saga.OrderSagaOrchestrator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * RabbitMQ message handler for Saga responses from Wallet Service
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SagaResponseHandler {

    private final OrderSagaOrchestrator sagaOrchestrator;

    /**
     * Handle funds reserved response
     */
    @RabbitListener(queues = RabbitMQConstants.ORDER_RESPONSE_QUEUE)
    public void handleSagaResponse(Object response, Message message, Channel channel) throws IOException {
        try {
            if (response instanceof FundsReservedEvent event) {
                log.info("Received FundsReservedEvent for saga {}", event.getSagaId());
                sagaOrchestrator.onFundsReserved(event.getSagaId(), event.getTransactionId());
                channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);

            } else if (response instanceof FundsReservationFailedEvent event) {
                log.warn("Received FundsReservationFailedEvent for saga {}: {}",
                        event.getSagaId(), event.getReason());
                sagaOrchestrator.onFundsReservationFailed(event.getSagaId(), event.getReason());
                channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);

            } else {
                log.warn("Unknown message type received: {}", response.getClass().getName());
                channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
            }
        } catch (Exception e) {
            log.error("Error processing saga response", e);
            // Requeue for retry
            channel.basicNack(message.getMessageProperties().getDeliveryTag(), false, true);
        }
    }
}

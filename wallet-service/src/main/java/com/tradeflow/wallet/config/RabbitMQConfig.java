package com.tradeflow.wallet.config;

import com.tradeflow.common.constants.RabbitMQConstants;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.lang.NonNull;

/**
 * RabbitMQ configuration for Saga command messaging
 */
@Configuration
public class RabbitMQConfig {

    /**
     * JSON message converter for RabbitMQ
     */
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    /**
     * RabbitTemplate with JSON converter
     */
    @Bean
    public RabbitTemplate rabbitTemplate(@NonNull ConnectionFactory connectionFactory,
                                         @NonNull MessageConverter messageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter);
        return template;
    }

    /**
     * Listener container factory with Virtual Threads enabled
     */
    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            @NonNull ConnectionFactory connectionFactory,
            @NonNull MessageConverter messageConverter) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(messageConverter);
        factory.setAcknowledgeMode(AcknowledgeMode.MANUAL);
        factory.setPrefetchCount(10);

        // Enable Virtual Threads for consumers
        SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor("rabbit-vt-");
        executor.setVirtualThreads(true);
        factory.setTaskExecutor(executor);

        return factory;
    }

    // ============================================
    // Exchange Declarations
    // ============================================

    @Bean
    public DirectExchange walletExchange() {
        return new DirectExchange(RabbitMQConstants.WALLET_EXCHANGE, true, false);
    }

    @Bean
    public DirectExchange orderExchange() {
        return new DirectExchange(RabbitMQConstants.ORDER_EXCHANGE, true, false);
    }

    @Bean
    public DirectExchange userExchange() {
        return new DirectExchange(RabbitMQConstants.USER_EXCHANGE, true, false);
    }

    // ============================================
    // Queue Declarations
    // ============================================

    /**
     * CRITICAL: Queue for user created events from auth-service.
     * This bean ensures the queue exists before the listener tries to connect.
     */
    @Bean
    public Queue userCreatedQueue() {
        return QueueBuilder.durable(RabbitMQConstants.USER_CREATED_QUEUE).build();
    }

    @Bean
    public Binding userCreatedBinding() {
        return BindingBuilder.bind(userCreatedQueue())
                .to(userExchange())
                .with(RabbitMQConstants.ROUTING_USER_CREATED);
    }

    @Bean
    public Queue reserveQueue() {
        return QueueBuilder.durable(RabbitMQConstants.WALLET_RESERVE_QUEUE)
                .withArgument("x-dead-letter-exchange", RabbitMQConstants.DLX_EXCHANGE)
                .build();
    }

    @Bean
    public Queue releaseQueue() {
        return QueueBuilder.durable(RabbitMQConstants.WALLET_RELEASE_QUEUE)
                .withArgument("x-dead-letter-exchange", RabbitMQConstants.DLX_EXCHANGE)
                .build();
    }

    @Bean
    public Queue settleQueue() {
        return QueueBuilder.durable(RabbitMQConstants.WALLET_SETTLE_QUEUE)
                .withArgument("x-dead-letter-exchange", RabbitMQConstants.DLX_EXCHANGE)
                .build();
    }

    // ============================================
    // Bindings
    // ============================================

    @Bean
    public Binding reserveBinding() {
        return BindingBuilder.bind(reserveQueue())
                .to(walletExchange())
                .with(RabbitMQConstants.ROUTING_WALLET_RESERVE);
    }

    @Bean
    public Binding releaseBinding() {
        return BindingBuilder.bind(releaseQueue())
                .to(walletExchange())
                .with(RabbitMQConstants.ROUTING_WALLET_RELEASE);
    }

    @Bean
    public Binding settleBinding() {
        return BindingBuilder.bind(settleQueue())
                .to(walletExchange())
                .with(RabbitMQConstants.ROUTING_WALLET_SETTLE);
    }

    // ============================================
    // Dead Letter Queue
    // ============================================

    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange(RabbitMQConstants.DLX_EXCHANGE, true, false);
    }

    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder.durable(RabbitMQConstants.DLQ_QUEUE).build();
    }

    @Bean
    public Binding deadLetterBinding() {
        return BindingBuilder.bind(deadLetterQueue())
                .to(deadLetterExchange())
                .with("#");
    }
}

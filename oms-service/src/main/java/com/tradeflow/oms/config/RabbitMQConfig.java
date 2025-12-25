package com.tradeflow.oms.config;

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

/**
 * RabbitMQ configuration for OMS Saga messaging
 */
@Configuration
public class RabbitMQConfig {

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        return template;
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(jsonMessageConverter());
        factory.setAcknowledgeMode(AcknowledgeMode.MANUAL);
        factory.setPrefetchCount(10);

        // Enable Virtual Threads
        SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor("rabbit-oms-");
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

    // ============================================
    // Queue Declarations (OMS listens to responses)
    // ============================================

    @Bean
    public Queue orderResponseQueue() {
        return QueueBuilder.durable(RabbitMQConstants.ORDER_RESPONSE_QUEUE)
                .withArgument("x-dead-letter-exchange", RabbitMQConstants.DLX_EXCHANGE)
                .build();
    }

    // ============================================
    // Bindings
    // ============================================

    @Bean
    public Binding orderResponseBinding() {
        return BindingBuilder.bind(orderResponseQueue())
                .to(orderExchange())
                .with(RabbitMQConstants.ROUTING_ORDER_RESPONSE);
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

package com.tradeflow.auth.config;

import com.tradeflow.common.constants.RabbitMQConstants;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ configuration for Auth Service.
 * Publishes UserCreatedEvent to wallet-service.
 */
@Configuration
public class RabbitMQConfig {

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jsonMessageConverter());
        return rabbitTemplate;
    }

    // Exchange for user events
    @Bean
    public DirectExchange userExchange() {
        return new DirectExchange(RabbitMQConstants.USER_EXCHANGE);
    }

    // Queue for user created events (consumed by wallet-service)
    @Bean
    public Queue userCreatedQueue() {
        return QueueBuilder.durable(RabbitMQConstants.USER_CREATED_QUEUE).build();
    }

    // Binding for user created events
    @Bean
    public Binding userCreatedBinding(Queue userCreatedQueue, DirectExchange userExchange) {
        return BindingBuilder.bind(userCreatedQueue)
                .to(userExchange)
                .with(RabbitMQConstants.ROUTING_USER_CREATED);
    }
}

package com.tradeflow.matching.config;

import com.tradeflow.common.constants.RabbitMQConstants;
import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ configuration for receiving orders from OMS.
 */
@Configuration
public class RabbitMQConfig {

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public DirectExchange orderExchange() {
        return new DirectExchange(RabbitMQConstants.ORDER_EXCHANGE, true, false);
    }

    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange(RabbitMQConstants.DLX_EXCHANGE, true, false);
    }

    @Bean
    public Queue matchingOrderQueue() {
        return QueueBuilder.durable(RabbitMQConstants.MATCHING_ORDER_QUEUE)
                .withArgument("x-dead-letter-exchange", RabbitMQConstants.DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", RabbitMQConstants.MATCHING_ORDER_QUEUE + ".dlq")
                .build();
    }

    @Bean
    public Queue matchingOrderDlq() {
        return QueueBuilder.durable(RabbitMQConstants.MATCHING_ORDER_QUEUE + ".dlq").build();
    }

    @Bean
    public Binding matchingOrderBinding() {
        return BindingBuilder.bind(matchingOrderQueue())
                .to(orderExchange())
                .with(RabbitMQConstants.ROUTING_ORDER_TO_MATCHING);
    }

    @Bean
    public Binding matchingOrderDlqBinding() {
        return BindingBuilder.bind(matchingOrderDlq())
                .to(deadLetterExchange())
                .with(RabbitMQConstants.MATCHING_ORDER_QUEUE + ".dlq");
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(jsonMessageConverter());
        factory.setAcknowledgeMode(AcknowledgeMode.MANUAL); // manual ack aligns with listener basicAck/basicNack
        factory.setPrefetchCount(10);
        return factory;
    }
}

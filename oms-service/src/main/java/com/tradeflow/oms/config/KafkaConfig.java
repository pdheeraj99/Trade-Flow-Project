package com.tradeflow.oms.config;

import com.tradeflow.common.constants.KafkaTopics;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Kafka configuration for OMS - produces orders to matching engine
 */
@Configuration
public class KafkaConfig {

    /**
     * Topic for orders going to matching engine
     * Partitioned by symbol for ordered processing
     */
    @Bean
    public NewTopic ordersToMatchingTopic() {
        return TopicBuilder.name(KafkaTopics.ORDERS_TO_MATCHING)
                .partitions(3) // Partition by symbol
                .replicas(1) // Single replica for dev
                .build();
    }

    /**
     * Topic for trade execution events from matching engine
     */
    @Bean
    public NewTopic tradesExecutedTopic() {
        return TopicBuilder.name(KafkaTopics.TRADES_EXECUTED)
                .partitions(3)
                .replicas(1)
                .build();
    }

    /**
     * Topic for order book updates
     */
    @Bean
    public NewTopic orderBookUpdatesTopic() {
        return TopicBuilder.name(KafkaTopics.ORDERBOOK_UPDATES)
                .partitions(3)
                .replicas(1)
                .build();
    }
}

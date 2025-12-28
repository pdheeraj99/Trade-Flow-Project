package com.tradeflow.matching.config;

import com.tradeflow.common.constants.KafkaTopics;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.*;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Kafka configuration for Matching Engine.
 * Uses Platform Threads (not Virtual Threads) for CPU-bound processing.
 */
@Configuration
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;

    // ============================================
    // Producer Configuration
    // ============================================

    @Bean
    public ProducerFactory<String, Object> producerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        config.put(ProducerConfig.ACKS_CONFIG, "all");
        config.put(ProducerConfig.RETRIES_CONFIG, 3);
        config.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        return Objects.requireNonNull(new DefaultKafkaProducerFactory<>(config));
    }

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate() {
        return new KafkaTemplate<>(Objects.requireNonNull(producerFactory()));
    }

    // ============================================
    // Consumer Configuration
    // ============================================

    @Bean
    public ConsumerFactory<String, Object> consumerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ConsumerConfig.GROUP_ID_CONFIG, "matching-engine");
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        config.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        config.put(JsonDeserializer.TRUSTED_PACKAGES, "com.tradeflow.*");
        return Objects.requireNonNull(new DefaultKafkaConsumerFactory<>(config));
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, Object> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(Objects.requireNonNull(consumerFactory()));
        factory.setConcurrency(3); // 3 consumers for 3 partitions
        factory.getContainerProperties().setAckMode(
                org.springframework.kafka.listener.ContainerProperties.AckMode.MANUAL_IMMEDIATE);
        return factory;
    }

    // ============================================
    // Topic Declarations
    // ============================================

    @Bean
    public NewTopic ordersToMatchingTopic() {
        return TopicBuilder.name(KafkaTopics.ORDERS_TO_MATCHING)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic tradesExecutedTopic() {
        return TopicBuilder.name(KafkaTopics.TRADES_EXECUTED)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic orderBookUpdatesTopic() {
        return TopicBuilder.name(KafkaTopics.ORDERBOOK_UPDATES)
                .partitions(3)
                .replicas(1)
                .build();
    }
}

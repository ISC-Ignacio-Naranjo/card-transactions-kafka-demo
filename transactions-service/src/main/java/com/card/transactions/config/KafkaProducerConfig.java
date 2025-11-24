package com.card.transactions.config;

import com.card.transactions.event.TransactionCreatedEvent;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JacksonJsonSerializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
@Slf4j
public class KafkaProducerConfig {

    @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;

    @Bean
    public ProducerFactory<String, TransactionCreatedEvent> transactionCreatedProducerFactory() {
        if (bootstrapServers == null || bootstrapServers.isBlank()) {
            throw new IllegalStateException("spring.kafka.bootstrap-servers is not configured");
        }

        log.info("Using Kafka bootstrap servers: {}", bootstrapServers);

        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JacksonJsonSerializer.class);

        return new DefaultKafkaProducerFactory<>(props);
    }

    @Bean
    public KafkaTemplate<String, TransactionCreatedEvent> transactionCreatedKafkaTemplate(
            ProducerFactory<String, TransactionCreatedEvent> transactionCreatedProducerFactory
    ) {
        return new KafkaTemplate<>(transactionCreatedProducerFactory);
    }
}

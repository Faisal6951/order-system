package com.orderplatform.order_service.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    @Bean
    public NewTopic orderPlacedTopic() {
        return TopicBuilder.name("order.placed").partitions(1).replicas(1).build();
    }

    @Bean
    public NewTopic paymentConfirmedTopic() {
        return TopicBuilder.name("payment.confirmed").partitions(1).replicas(1).build();
    }

    @Bean
    public NewTopic paymentFailedTopic() {
        return TopicBuilder.name("payment.failed").partitions(1).replicas(1).build();
    }
}
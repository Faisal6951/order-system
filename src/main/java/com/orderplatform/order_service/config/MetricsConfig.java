package com.orderplatform.order_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.orderplatform.order_service.repository.OutboxEventRepository;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.binder.MeterBinder;

@Configuration
public class MetricsConfig {

    @Bean
    public MeterBinder outboxMetrics(OutboxEventRepository repository) {
        return registry -> Gauge
                .builder("outbox.pending", repository, OutboxEventRepository::countByPublishedFalse)
                .description("Outbox events waiting to be published to Kafka")
                .register(registry);
    }
}
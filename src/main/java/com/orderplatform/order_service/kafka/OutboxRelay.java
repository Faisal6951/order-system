package com.orderplatform.order_service.kafka;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.orderplatform.order_service.model.OutboxEvent;
import com.orderplatform.order_service.repository.OutboxEventRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxRelay {

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Scheduled(fixedDelay = 1000)
    @Transactional
    public void publishPendingEvents() {

        List<OutboxEvent> pending = outboxEventRepository.findByPublishedFalseOrderByIdAsc();

        for (OutboxEvent event : pending) {
            try {
                kafkaTemplate
                        .send(event.getTopic(), event.getAggregateId(), event.getPayload())
                        .get(5, TimeUnit.SECONDS);

                event.setPublished(true);
                log.info("Outbox event {} published to {}", event.getId(), event.getTopic());

            } catch (Exception e) {
                log.error("Failed to publish outbox event {}, will retry next run", event.getId(), e);
                break;
            }
        }
    }
}

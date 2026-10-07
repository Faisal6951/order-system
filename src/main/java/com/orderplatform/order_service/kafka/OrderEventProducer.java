package com.orderplatform.order_service.kafka;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import com.orderplatform.order_service.event.OrderPlacedEvent;

import lombok.RequiredArgsConstructor;
import tools.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
public class OrderEventProducer {

    private static final String ORDER_PLACED_TOPIC = "order.placed";

    private final KafkaTemplate<String, String> kafkaTemplate;

    private final ObjectMapper objectMapper;

    public void publishOrderPlacedEvent(OrderPlacedEvent event) {
        try {
            String message = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(ORDER_PLACED_TOPIC, message);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize order event", e);
        }
    }
}
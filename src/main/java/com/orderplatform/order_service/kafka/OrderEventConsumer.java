package com.orderplatform.order_service.kafka;

import com.orderplatform.order_service.event.OrderPlacedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
public class OrderEventConsumer {

    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "order.placed", groupId = "order-service-group")
    public void handleOrderPlaced(String message) {
        try {
            OrderPlacedEvent event = objectMapper.readValue(message, OrderPlacedEvent.class);
            System.out.println("ORDER CONSUMER: Processing order " + event.getOrderId()
                    + " for " + event.getCustomerName());
        } catch (Exception e) {
            throw new RuntimeException("Failed to process order event", e);
        }
    }
}
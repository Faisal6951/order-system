package com.orderplatform.order_service.kafka;

import com.orderplatform.order_service.event.OrderPlacedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
public class NotificationEventConsumer {

    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "order.placed", groupId = "notification-service-group")
    public void handleOrderPlaced(String message) {
        try {
            OrderPlacedEvent event = objectMapper.readValue(message, OrderPlacedEvent.class);
            System.out.println("Notification CONSUMER: Sending email to " + event.getCustomerName() + " for order "
                    + event.getOrderId());
        } catch (Exception e) {
            throw new RuntimeException("Failed to process order event", e);
        }
    }
}
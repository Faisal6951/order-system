package com.orderplatform.order_service.kafka;

import org.springframework.context.annotation.Lazy;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import com.orderplatform.order_service.service.OrderService;

// import lombok.RequiredArgsConstructor;

@Service
// @RequiredArgsConstructor
public class PaymentResultConsumer {
    public OrderService orderService;

    public PaymentResultConsumer(@Lazy OrderService orderService) {
        this.orderService = orderService;
    }

    @KafkaListener(topics = "payment.confirmed", groupId = "order-update-group")
    public void handlePaymentConfirmed(String message) {
        try {
            Long orderId = Long.parseLong(message);
            orderService.updateOrderStatus(orderId, "CONFIRMED");
        } catch (Exception e) {
            throw new RuntimeException("Failed to process payment confirmed event", e);
        }
    }

    @KafkaListener(topics = "payment.failed", groupId = "order-update-group")
    public void handlePaymentFailed(String message) {
        try {
            Long orderId = Long.parseLong(message);
            orderService.updateOrderStatus(orderId, "CANCELLED");
        } catch (Exception e) {
            throw new RuntimeException("Failed to process payment failed event", e);
        }
    }
}

package com.orderplatform.order_service.kafka;

import com.orderplatform.order_service.event.OrderPlacedEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import io.micrometer.core.instrument.MeterRegistry;

import tools.jackson.databind.ObjectMapper;

@Service
@Slf4j
@RequiredArgsConstructor
public class PaymentEventConsumer {

    private final ObjectMapper objectMapper;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final StringRedisTemplate stringRedisTemplate;

    private final MeterRegistry meterRegistry;

    @KafkaListener(topics = "order.placed", groupId = "payment-service-group")
    public void handleOrderPlaced(String message) {
        try {

            OrderPlacedEvent event = objectMapper.readValue(message, OrderPlacedEvent.class);

            String orderId = String.valueOf(event.getOrderId());
            String key = "payment:processed:" + event.getOrderId();
            Boolean first = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", 24, TimeUnit.HOURS);

            if (!Boolean.TRUE.equals(first)) {
                log.info("Duplicate order.placed for order {}, skipping", event.getOrderId());
                meterRegistry.counter("payments.duplicates").increment(); // inside the duplicate check
                return;
            }
            System.out.println("Payment CONSUMER: Processing order " + event.getOrderId()
                    + " for " + event.getCustomerName() + event.getTotalPrice() + event.getProductTitle());

            boolean paymentSuccess = Math.random() > 0.2; // 80% success, 20% failure

            if (paymentSuccess) {

                kafkaTemplate.send("payment.confirmed", orderId,
                        orderId);
                meterRegistry.counter("payments.processed", "result", "confirmed").increment(); // success branch
            } else {

                kafkaTemplate.send("payment.failed", orderId, orderId);
                meterRegistry.counter("payments.processed", "result", "failed").increment(); // failure branch
            }

        } catch (Exception e) {
            throw new RuntimeException("Failed to process order event", e);
        }
    }
}
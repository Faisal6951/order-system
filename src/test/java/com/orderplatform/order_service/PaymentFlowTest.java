package com.orderplatform.order_service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import java.util.function.BooleanSupplier;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.annotation.DirtiesContext;

import com.orderplatform.order_service.model.OrderStatus;
import com.orderplatform.order_service.model.OutboxEvent;
import com.orderplatform.order_service.repository.OrderRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.data.redis.core.StringRedisTemplate;

@SpringBootTest(properties = {
        "ratelimit.requests-per-minute=100000",
        "spring.kafka.listener.auto-startup=true"
})
@DirtiesContext
class PaymentFlowTest extends ApiTestSupport {

    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private MeterRegistry meterRegistry;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Test
    void orderIsPaidThroughOutboxAndKafka() throws Exception {
        Long productId = createProduct("product-" + UUID.randomUUID());
        MockHttpServletResponse response = placeOrder(productId, 1, UUID.randomUUID().toString());
        assertThat(response.getStatus()).isEqualTo(201);
        Long orderId = Long.valueOf(idOf(response.getContentAsString()));

        // try {
        // waitUntil("order leaves PENDING", () -> statusOf(orderId) !=
        // OrderStatus.PENDING);
        // } catch (AssertionError e) {
        // throw new AssertionError(e.getMessage() + " | " + diagnose(orderId), e);
        // }
        awaitPayment(orderId);

        OrderStatus finalStatus = statusOf(orderId);
        assertThat(finalStatus).isIn(OrderStatus.CONFIRMED, OrderStatus.CANCELLED);
        assertThat(outboxRow(orderId).getPublished()).isTrue();
        // CONFIRMED keeps the stock; CANCELLED (payment failed) gives it back
        assertThat(stock(productId)).isEqualTo(finalStatus == OrderStatus.CONFIRMED ? 9 : 10);
    }

    @Test
    void duplicateOrderPlacedEventIsPaidOnlyOnce() throws Exception {
        Long productId = createProduct("product-" + UUID.randomUUID());
        MockHttpServletResponse response = placeOrder(productId, 1, UUID.randomUUID().toString());
        Long orderId = Long.valueOf(idOf(response.getContentAsString()));

        // waitUntil("first payment finishes", () -> statusOf(orderId) !=
        // OrderStatus.PENDING);
        awaitPayment(orderId);
        OrderStatus statusBefore = statusOf(orderId);
        double processedBefore = totalPaymentsProcessed();
        double duplicatesBefore = meterRegistry.counter("payments.duplicates").count();

        // simulate the relay crashing after Kafka's ack, before it saved published =
        // true
        OutboxEvent row = outboxRow(orderId);
        row.setPublished(false);
        outboxEventRepository.save(row);

        waitUntil("relay re-sends the event", () -> outboxRow(orderId).getPublished());
        waitUntil("duplicate detected",
                () -> meterRegistry.counter("payments.duplicates").count() > duplicatesBefore);
        Thread.sleep(2000);

        assertThat(totalPaymentsProcessed()).isEqualTo(processedBefore);
        assertThat(statusOf(orderId)).isEqualTo(statusBefore);
    }

    private OrderStatus statusOf(Long orderId) {
        return orderRepository.findById(orderId).orElseThrow().getStatus();
    }

    private OutboxEvent outboxRow(Long orderId) {
        return outboxEventRepository.findAll().stream()
                .filter(e -> e.getAggregateId().equals(String.valueOf(orderId))
                        && e.getTopic().equals("order.placed"))
                .findFirst().orElseThrow();
    }

    private double totalPaymentsProcessed() {
        return meterRegistry.find("payments.processed").counters().stream()
                .mapToDouble(Counter::count).sum();
    }

    private void waitUntil(String what, BooleanSupplier condition) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 30_000;
        while (System.currentTimeMillis() < deadline) {
            if (condition.getAsBoolean()) {
                return;
            }
            Thread.sleep(200);
        }
        throw new AssertionError("Timed out waiting for: " + what);
    }

    private String diagnose(Long orderId) {
        return "status=" + statusOf(orderId)
                + ", outboxPublished=" + outboxRow(orderId).getPublished()
                + ", paymentKeyInRedis=" + stringRedisTemplate.hasKey("payment:processed:" + orderId)
                + ", paymentsProcessed=" + totalPaymentsProcessed();
    }

    private void awaitPayment(Long orderId) throws InterruptedException {
        long start = System.currentTimeMillis();
        long publishedAt = -1, decidedAt = -1, doneAt = -1;

        while (System.currentTimeMillis() - start < 90_000) {
            long elapsed = System.currentTimeMillis() - start;
            if (publishedAt < 0 && outboxRow(orderId).getPublished()) {
                publishedAt = elapsed;
            }
            if (decidedAt < 0 && Boolean.TRUE.equals(
                    stringRedisTemplate.hasKey("payment:processed:" + orderId))) {
                decidedAt = elapsed;
            }
            if (statusOf(orderId) != OrderStatus.PENDING) {
                doneAt = elapsed;
                break;
            }
            Thread.sleep(250);
        }

        System.out.printf(
                "TIMELINE order %d: outbox published at %d ms, payment decided at %d ms, status changed at %d ms%n",
                orderId, publishedAt, decidedAt, doneAt);

        if (doneAt < 0) {
            throw new AssertionError("Order still PENDING after 90 s | " + diagnose(orderId));
        }
    }
}
package com.orderplatform.order_service;

import java.security.SecureRandom;
import java.util.Base64;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;

@AutoConfigureMockMvc
@SpringBootTest(properties = {
        "ratelimit.requests-per-minute=100000",
        "spring.kafka.listener.auto-startup=false"
})
public abstract class IntegrationTestBase {

    @ServiceConnection
    static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16");

    @ServiceConnection(name = "redis")
    static final GenericContainer<?> redis = new GenericContainer<>("redis:7").withExposedPorts(6379);

    @ServiceConnection
    static final KafkaContainer kafka = new KafkaContainer("apache/kafka:3.8.0");

    static {
        postgres.start();
        redis.start();
        kafka.start();
    }

    @DynamicPropertySource
    static void jwtSecret(DynamicPropertyRegistry registry) {
        byte[] bytes = new byte[64];
        new SecureRandom().nextBytes(bytes);
        String secret = Base64.getEncoder().encodeToString(bytes);
        registry.add("jwt.secret", () -> secret);
    }
}
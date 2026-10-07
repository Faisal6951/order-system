package com.orderplatform.order_service.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Entity

@Table(name = "outbox_events")
public class OutboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "this id is required")
    @NotBlank(message = "aggregatedId is required")
    @Column(nullable = false)
    private String aggregateId;

    @NotBlank(message = "topic is required")
    @Column(nullable = false)
    private String topic;

    @NotEmpty(message = "payload required")
    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;

    @NotNull(message = "published is required")
    @Column(nullable = false)
    private Boolean published;

    private LocalDateTime createdAt;

    @PrePersist
    public void prepersist() {
        this.createdAt = LocalDateTime.now();
        this.published = false;
    }

}

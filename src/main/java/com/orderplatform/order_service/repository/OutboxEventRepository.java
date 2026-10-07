package com.orderplatform.order_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.orderplatform.order_service.model.OutboxEvent;
import java.util.List;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {
    List<OutboxEvent> findByPublishedFalseOrderByIdAsc();

    long countByPublishedFalse();
}

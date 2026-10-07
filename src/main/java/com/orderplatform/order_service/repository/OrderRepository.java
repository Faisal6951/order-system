package com.orderplatform.order_service.repository;

import com.orderplatform.order_service.model.Order;

import io.lettuce.core.dynamic.annotation.Param;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    Page<Order> findByUserEmail(String userEmail, Pageable pageable);

    @Query("select o.product.id from Order o where o.id = :id")
    Optional<Long> findProductIdById(@Param("id") Long id);
}
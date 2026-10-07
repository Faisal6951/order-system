package com.orderplatform.order_service.repository;

import com.orderplatform.order_service.model.Product;

import io.lettuce.core.dynamic.annotation.Param;

import java.util.Collection;
import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ProductRepository extends JpaRepository<Product, Long> {

    boolean existsByProductTitle(String productTitle);

    @Query("select p.productTitle from Product p where p.productTitle in :titles")
    Set<String> findExistingTitles(@Param("titles") Collection<String> titles);
}
package com.orderplatform.order_service.dto;

import java.util.List;

import com.orderplatform.order_service.model.Product;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class BulkProductResponse {
    private int successCount;

    private int failedCount;

    private List<Product> insertedProducts;

    private List<BulkFailure> failures;
}

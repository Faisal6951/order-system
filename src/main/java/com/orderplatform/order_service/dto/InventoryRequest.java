package com.orderplatform.order_service.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class InventoryRequest {
    @NotNull(message = "product id is required")
    private Long productId;

    @NotNull(message = "provide stocks of product")
    private Integer stockAvailable;
}

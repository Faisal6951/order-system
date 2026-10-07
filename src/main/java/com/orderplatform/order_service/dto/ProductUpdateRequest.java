package com.orderplatform.order_service.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ProductUpdateRequest {
    @NotBlank(message = "please provide title of product")
    private String productTitle;

    @NotNull(message = "set price of product")
    @DecimalMin(value = "0.01", message = "Price must be greater than 0")
    private BigDecimal price;

    private String productDescription;
}

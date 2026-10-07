package com.orderplatform.order_service.exception;

public class InventoryNotFoundException extends RuntimeException {
    public InventoryNotFoundException(Long id) {
        super("Inventory not found for product: " + id);
    }
}
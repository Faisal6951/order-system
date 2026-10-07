package com.orderplatform.order_service.exception;

public class InsufficientStockException extends RuntimeException {
    public InsufficientStockException(int requested, int available) {
        super("Insufficient stock. Requested: " + requested + ", Available: " + available);
    }
}
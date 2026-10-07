package com.orderplatform.order_service.exception;

public class ProductAlreadyExistsException extends RuntimeException {
    public ProductAlreadyExistsException() {
        super("Product already exist");
    }
}
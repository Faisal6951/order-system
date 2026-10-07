package com.orderplatform.order_service.exception;

public class InvalidStatusTransitionException extends RuntimeException {
    public InvalidStatusTransitionException(String status) {
        super("Now that status won't be accepted please select these: " + status);
    }
}

package com.orderplatform.order_service.model;

import java.util.Set;

public enum OrderStatus {
    PENDING,
    CONFIRMED,
    PROCESSING,
    SHIPPED,
    DELIVERED,
    CANCELLED;

    private Set<OrderStatus> validTransitions;

    public boolean canTransitionTo(OrderStatus next) {
        return getValidTransitions().contains(next);
    }

    private Set<OrderStatus> getValidTransitions() {
        if (validTransitions == null) {
            validTransitions = switch (this) {
                case PENDING -> Set.of(CONFIRMED, CANCELLED);
                case CONFIRMED -> Set.of(PROCESSING, CANCELLED);
                case PROCESSING -> Set.of(SHIPPED);
                case SHIPPED -> Set.of(DELIVERED);
                case DELIVERED, CANCELLED -> Set.of();
            };
        }
        return validTransitions;
    }
}
package com.orderplatform.order_service.service;

import com.orderplatform.order_service.dto.OrderRequest;
import com.orderplatform.order_service.event.OrderPlacedEvent;
import com.orderplatform.order_service.exception.InsufficientStockException;
import com.orderplatform.order_service.exception.InvalidStatusTransitionException;
import com.orderplatform.order_service.exception.InventoryNotFoundException;
import com.orderplatform.order_service.exception.OrderNotFoundException;
import com.orderplatform.order_service.exception.ProductNotFoundException;
import com.orderplatform.order_service.model.Inventory;
import com.orderplatform.order_service.model.Order;
import com.orderplatform.order_service.model.OrderStatus;
import com.orderplatform.order_service.model.OutboxEvent;
import com.orderplatform.order_service.model.Product;
import com.orderplatform.order_service.model.User;
import com.orderplatform.order_service.repository.InventoryRepository;
import com.orderplatform.order_service.repository.OrderRepository;
import com.orderplatform.order_service.repository.OutboxEventRepository;
import com.orderplatform.order_service.repository.ProductRepository;
import com.orderplatform.order_service.repository.UserRepository;

import lombok.RequiredArgsConstructor;

// import com.fasterxml.jackson.core.JsonProcessingException;
// import com.fasterxml.jackson.databind.ObjectMapper;
import tools.jackson.databind.ObjectMapper;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.micrometer.core.instrument.MeterRegistry;

import java.util.concurrent.TimeUnit;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;

@Service
@RequiredArgsConstructor

public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final UserRepository userRepository;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final MeterRegistry meterRegistry;

    @Transactional
    public Order createOrder(OrderRequest request, String idempotencyKey) {
        String userEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        String redisKey = "idempotency:" + userEmail + ":" + idempotencyKey;

        String existingOrderId = (String) redisTemplate.opsForValue().get(redisKey);
        if (existingOrderId != null) {
            return orderRepository.findById(Long.parseLong(existingOrderId))
                    .orElseThrow(() -> new OrderNotFoundException(Long.parseLong(existingOrderId)));
        }

        if (!productRepository.existsById(request.getProductId())) {
            throw new ProductNotFoundException(request.getProductId());
        }

        Inventory inventory = inventoryRepository.findByProductIdWithLock(request.getProductId())
                .orElseThrow(() -> new InventoryNotFoundException(request.getProductId()));

        Product product = inventory.getProduct();

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Inventory not found User by this email: " + userEmail));

        int quantity = request.getQuantity();
        if (quantity > inventory.getStockAvaiable()) {
            throw new InsufficientStockException(request.getQuantity(), inventory.getStockAvaiable());
        }

        BigDecimal unitPrice = product.getPrice();

        BigDecimal totalPrice = unitPrice.multiply(BigDecimal.valueOf(quantity));

        Order order = new Order();
        order.setProduct(product);
        order.setCustomerName(user.getName());
        order.setUserEmail(userEmail);
        order.setQuantity(request.getQuantity());
        order.setUnitPrice(unitPrice);
        order.setTotalPrice(totalPrice);

        inventory.setStockAvaiable(inventory.getStockAvaiable() - request.getQuantity());

        inventoryRepository.save(inventory);

        Order savedOrder = orderRepository.save(order);

        OrderPlacedEvent event = new OrderPlacedEvent(
                savedOrder.getId(),
                savedOrder.getUserEmail(),
                product.getProductTitle(),
                savedOrder.getQuantity(),
                savedOrder.getUnitPrice(),
                savedOrder.getTotalPrice(),
                savedOrder.getStatus().name());

        OutboxEvent outboxEvent = new OutboxEvent();
        outboxEvent.setAggregateId(String.valueOf(savedOrder.getId()));
        outboxEvent.setTopic("order.placed");
        outboxEvent.setPayload(objectMapper.writeValueAsString(event));
        outboxEventRepository.save(outboxEvent);

        redisTemplate.opsForValue().set(
                redisKey,
                String.valueOf(savedOrder.getId()),
                24,
                TimeUnit.HOURS);

        meterRegistry.counter("orders.placed").increment();

        return savedOrder;
    }

    public Page<Order> getAllOrders(Pageable pageable) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Collection<? extends GrantedAuthority> userRole = auth.getAuthorities();
        String userEmail = auth.getName();

        if (userRole.stream().anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()))) {
            return orderRepository.findAll(pageable);
        } else {

            Page<Order> order = orderRepository.findByUserEmail(userEmail, pageable);
            return order;
        }
    }

    @Cacheable(key = "#id", value = "orders")
    public Order getOrderById(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException(id));
    }

    @Transactional
    @CacheEvict(key = "#id", value = "orders")
    public Order updateOrderStatus(Long id, String status) {

        Long productId = orderRepository.findProductIdById(id)
                .orElseThrow(() -> new OrderNotFoundException(id));

        // 1. lock first: nothing about this inventory row may be loaded before this
        // line
        Inventory inventory = inventoryRepository.findByProductIdWithLock(productId)
                .orElseThrow(() -> new InventoryNotFoundException(productId));

        // 2. only now load the order: fresh, and any competing change has already
        // committed
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException(id));

        OrderStatus newStatus = OrderStatus.valueOf(status.toUpperCase());

        if (!order.getStatus().canTransitionTo(newStatus)) {
            throw new InvalidStatusTransitionException(order.getStatus() + " → " + newStatus);
        }

        if (newStatus == OrderStatus.CANCELLED) {
            inventory.setStockAvaiable(inventory.getStockAvaiable() + order.getQuantity());
        }

        order.setStatus(newStatus);
        return order;
    }

    public void verifyAccess(Order order) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean admin = auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));

        if (!admin && !order.getUserEmail().equals(auth.getName())) {
            throw new OrderNotFoundException(order.getId());
        }
    }
}
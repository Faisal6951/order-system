package com.orderplatform.order_service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.orderplatform.order_service.model.OrderStatus;
import com.orderplatform.order_service.repository.OrderRepository;

class AccessControlTest extends ApiTestSupport {

    @Autowired
    private OrderRepository orderRepository;

    @Test
    void customerCannotChangeOrderStatus() throws Exception {
        Long orderId = newOrderId();

        mockMvc.perform(patch("/api/orders/" + orderId + "/status")
                .param("status", "CONFIRMED")
                .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());

        assertThat(orderRepository.findById(orderId).orElseThrow().getStatus())
                .isEqualTo(OrderStatus.PENDING);
    }

    @Test
    void adminCanChangeOrderStatus() throws Exception {
        Long orderId = newOrderId();

        mockMvc.perform(patch("/api/orders/" + orderId + "/status")
                .param("status", "CONFIRMED")
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        assertThat(orderRepository.findById(orderId).orElseThrow().getStatus())
                .isEqualTo(OrderStatus.CONFIRMED);
    }

    @Test
    void orderCanOnlyBeReadByItsOwnerOrAnAdmin() throws Exception {
        Long orderId = newOrderId();
        String strangerToken = registerAndLogin(false);

        readOrder(orderId, userToken, 200); // the owner, which also fills the Redis cache
        readOrder(orderId, adminToken, 200);
        readOrder(orderId, strangerToken, 404); // must still be refused while the order is cached
    }

    private Long newOrderId() throws Exception {
        Long productId = createProduct("product-" + UUID.randomUUID());
        String body = placeOrder(productId, 1, UUID.randomUUID().toString()).getContentAsString();
        return Long.valueOf(idOf(body));
    }

    private void readOrder(Long orderId, String token, int expectedStatus) throws Exception {
        mockMvc.perform(get("/api/orders/" + orderId)
                .header("Authorization", "Bearer " + token))
                .andExpect(status().is(expectedStatus));
    }
}
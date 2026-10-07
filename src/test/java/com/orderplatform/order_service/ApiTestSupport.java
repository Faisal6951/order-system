package com.orderplatform.order_service;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;

import com.jayway.jsonpath.JsonPath;
import com.orderplatform.order_service.model.User;
import com.orderplatform.order_service.model.UserRole;
import com.orderplatform.order_service.repository.InventoryRepository;
import com.orderplatform.order_service.repository.OutboxEventRepository;
import com.orderplatform.order_service.repository.ProductRepository;
import com.orderplatform.order_service.repository.UserRepository;

abstract class ApiTestSupport extends IntegrationTestBase {

    @Autowired
    protected MockMvc mockMvc;
    @Autowired
    protected UserRepository userRepository;
    @Autowired
    protected ProductRepository productRepository;
    @Autowired
    protected InventoryRepository inventoryRepository;
    @Autowired
    protected OutboxEventRepository outboxEventRepository;

    protected String adminToken;
    protected String userToken;

    @BeforeEach
    void setUpTokens() throws Exception {
        adminToken = registerAndLogin(true);
        userToken = registerAndLogin(false);
    }

    protected String registerAndLogin(boolean admin) throws Exception {
        String email = "user-" + UUID.randomUUID() + "@test.com";
        String credentials = """
                {"name":"Test User","email":"%s","password":"Password123!"}
                """.formatted(email);

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON).content(credentials))
                .andExpect(status().isCreated());

        if (admin) {
            User user = userRepository.findByEmail(email).orElseThrow();
            user.setUserRole(UserRole.ADMIN);
            userRepository.save(user);
        }

        String login = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON).content(credentials))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(login, "$.token");
    }

    protected Long createProduct(String title) throws Exception {
        mockMvc.perform(post("/api/products")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"productTitle":"%s","price":100.00,"productDescription":"test"}
                        """.formatted(title)))
                .andExpect(status().is2xxSuccessful());

        return productRepository.findAll().stream()
                .filter(p -> title.equals(p.getProductTitle()))
                .findFirst().orElseThrow().getId();
    }

    protected MockHttpServletResponse placeOrder(Long productId, int quantity, String idempotencyKey) throws Exception {
        return mockMvc.perform(post("/api/orders")
                .header("Authorization", "Bearer " + userToken)
                .header("Idempotency-Key", idempotencyKey)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"productId":%d,"quantity":%d}
                        """.formatted(productId, quantity)))
                .andReturn().getResponse();
    }

    protected int stock(Long productId) {
        return inventoryRepository.findByProductId(productId).orElseThrow().getStockAvaiable();
    }

    protected String idOf(String body) {
        return String.valueOf(JsonPath.<Object>read(body, "$.id"));
    }
}
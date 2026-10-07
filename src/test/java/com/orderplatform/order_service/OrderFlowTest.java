package com.orderplatform.order_service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

// import com.jayway.jsonpath.JsonPath;
// import com.orderplatform.order_service.model.User;
// import com.orderplatform.order_service.model.UserRole;
// import com.orderplatform.order_service.repository.InventoryRepository;
// import com.orderplatform.order_service.repository.OutboxEventRepository;
// import com.orderplatform.order_service.repository.ProductRepository;
// import com.orderplatform.order_service.repository.UserRepository;

@TestPropertySource(properties = "spring.kafka.listener.auto-startup=false")
class OrderFlowTest extends ApiTestSupport {

    private String adminToken;
    private String userToken;

    @BeforeEach
    void setUp() throws Exception {
        adminToken = registerAndLogin(true);
        userToken = registerAndLogin(false);
    }

    @Test
    void placingAnOrderReducesStockAndWritesOutboxRow() throws Exception {
        Long productId = createProduct("product-" + UUID.randomUUID());
        assertThat(stock(productId)).isEqualTo(10);

        MockHttpServletResponse response = placeOrder(productId, 3, UUID.randomUUID().toString());
        assertThat(response.getStatus()).isEqualTo(201);
        String orderId = idOf(response.getContentAsString());

        assertThat(stock(productId)).isEqualTo(7);
        assertThat(outboxEventRepository.findAll())
                .anyMatch(e -> e.getAggregateId().equals(orderId) && e.getTopic().equals("order.placed"));
    }

    @Test
    void sameIdempotencyKeyCreatesOnlyOneOrder() throws Exception {
        Long productId = createProduct("product-" + UUID.randomUUID());
        String key = UUID.randomUUID().toString();

        MockHttpServletResponse first = placeOrder(productId, 2, key);
        MockHttpServletResponse second = placeOrder(productId, 2, key);

        assertThat(first.getStatus()).isEqualTo(201);
        assertThat(second.getStatus()).isEqualTo(201);
        assertThat(idOf(second.getContentAsString())).isEqualTo(idOf(first.getContentAsString()));
        assertThat(stock(productId)).isEqualTo(8);
    }

    @Test
    void concurrentOrdersNeverOversellStock() throws Exception {
        Long productId = createProduct("product-" + UUID.randomUUID());
        assertThat(stock(productId)).isEqualTo(10);

        int attempts = 20;
        ExecutorService pool = Executors.newFixedThreadPool(attempts);
        CountDownLatch startGate = new CountDownLatch(1);
        List<Future<Integer>> futures = new ArrayList<>();

        for (int i = 0; i < attempts; i++) {
            futures.add(pool.submit(() -> {
                startGate.await();
                return placeOrder(productId, 1, UUID.randomUUID().toString()).getStatus();
            }));
        }
        startGate.countDown();

        int created = 0;
        int rejected = 0;
        for (Future<Integer> future : futures) {
            int status = future.get(60, TimeUnit.SECONDS);
            if (status == 201)
                created++;
            else if (status == 400)
                rejected++;
        }
        pool.shutdown();

        assertThat(created).isEqualTo(10);
        assertThat(rejected).isEqualTo(10);
        assertThat(stock(productId)).isZero();
    }

    /*
     * 
     * 
     * private String registerAndLogin(boolean admin) throws Exception {
     * String email = "user-" + UUID.randomUUID() + "@test.com";
     * String credentials = """
     * {"name":"Test User","email":"%s","password":"Password123!"}
     * """.formatted(email);
     * 
     * mockMvc.perform(post("/api/auth/register")
     * .contentType(MediaType.APPLICATION_JSON).content(credentials))
     * .andExpect(status().isCreated());
     * 
     * if (admin) {
     * User user = userRepository.findByEmail(email).orElseThrow();
     * user.setUserRole(UserRole.ADMIN);
     * userRepository.save(user);
     * }
     * 
     * String login = mockMvc.perform(post("/api/auth/login")
     * .contentType(MediaType.APPLICATION_JSON).content(credentials))
     * .andExpect(status().isOk())
     * .andReturn().getResponse().getContentAsString();
     * return JsonPath.read(login, "$.token");
     * }
     * 
     * private Long createProduct(String title) throws Exception {
     * mockMvc.perform(post("/api/products")
     * .header("Authorization", "Bearer " + adminToken)
     * .contentType(MediaType.APPLICATION_JSON)
     * .content("""
     * {"productTitle":"%s","price":100.00,"productDescription":"test"}
     * """.formatted(title)))
     * .andExpect(status().is2xxSuccessful());
     * 
     * return productRepository.findAll().stream()
     * .filter(p -> title.equals(p.getProductTitle()))
     * .findFirst().orElseThrow().getId();
     * }
     * 
     * private MockHttpServletResponse placeOrder(Long productId, int quantity,
     * String idempotencyKey) throws Exception {
     * return mockMvc.perform(post("/api/orders")
     * .header("Authorization", "Bearer " + userToken)
     * .header("Idempotency-Key", idempotencyKey)
     * .contentType(MediaType.APPLICATION_JSON)
     * .content("""
     * {"productId":%d,"quantity":%d}
     * """.formatted(productId, quantity)))
     * .andReturn().getResponse();
     * }
     * 
     * private int stock(Long productId) {
     * return
     * inventoryRepository.findByProductId(productId).orElseThrow().getStockAvaiable
     * ();
     * }
     * 
     * private String idOf(String body) {
     * return String.valueOf(JsonPath.<Object>read(body, "$.id"));
     * }
     */
    @Test
    void concurrentCancellationsRestoreEveryUnit() throws Exception {
        Long productId = createProduct("product-" + UUID.randomUUID());

        List<String> orderIds = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            MockHttpServletResponse r = placeOrder(productId, 1, UUID.randomUUID().toString());
            assertThat(r.getStatus()).isEqualTo(201);
            orderIds.add(idOf(r.getContentAsString()));
        }
        assertThat(stock(productId)).isZero();

        List<Callable<Integer>> tasks = orderIds.stream()
                .<Callable<Integer>>map(id -> () -> cancel(id).getStatus())
                .toList();
        List<Integer> statuses = runConcurrently(tasks);

        assertThat(statuses).containsOnly(200);
        assertThat(stock(productId)).isEqualTo(10);
    }

    @Test
    void cancellingTheSameOrderManyTimesRestoresStockOnce() throws Exception {
        Long productId = createProduct("product-" + UUID.randomUUID());
        MockHttpServletResponse placed = placeOrder(productId, 3, UUID.randomUUID().toString());
        String orderId = idOf(placed.getContentAsString());
        assertThat(stock(productId)).isEqualTo(7);

        List<Callable<Integer>> tasks = IntStream.range(0, 5)
                .<Callable<Integer>>mapToObj(i -> () -> cancel(orderId).getStatus())
                .toList();
        List<Integer> statuses = runConcurrently(tasks);

        assertThat(statuses.stream().filter(s -> s == 200).count()).isEqualTo(1);
        assertThat(stock(productId)).isEqualTo(10);
    }

    private MockHttpServletResponse cancel(String orderId) throws Exception {
        return mockMvc.perform(patch("/api/orders/" + orderId + "/status")
                .param("status", "CANCELLED")
                .header("Authorization", "Bearer " + adminToken))
                .andReturn().getResponse();
    }

    private List<Integer> runConcurrently(List<Callable<Integer>> tasks) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(tasks.size());
        CountDownLatch gate = new CountDownLatch(1);
        List<Future<Integer>> futures = new ArrayList<>();
        for (Callable<Integer> task : tasks) {
            futures.add(pool.submit(() -> {
                gate.await();
                return task.call();
            }));
        }
        gate.countDown();
        List<Integer> results = new ArrayList<>();
        for (Future<Integer> future : futures) {
            results.add(future.get(60, TimeUnit.SECONDS));
        }
        pool.shutdown();
        return results;
    }
}
package com.orderplatform.order_service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.jayway.jsonpath.JsonPath;

class AuthFlowTest extends IntegrationTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void fullAuthLifecycle() throws Exception {
        String email = "user-" + UUID.randomUUID() + "@test.com";

        String registered = send("/api/auth/register", """
                {"name":"Test User","email":"%s","password":"Password123!"}
                """.formatted(email), 201);
        assertThat(field(registered, "$.token")).isNotBlank();
        assertThat(field(registered, "$.refreshToken")).isNotBlank();

        String loggedIn = send("/api/auth/login", """
                {"email":"%s","password":"Password123!"}
                """.formatted(email), 200);
        String refresh1 = field(loggedIn, "$.refreshToken");

        String refreshed = send("/api/auth/refresh", refreshJson(refresh1), 200);
        String refresh2 = field(refreshed, "$.refreshToken");
        assertThat(refresh2).isNotEqualTo(refresh1);

        // rotation: the old token is dead
        send("/api/auth/refresh", refreshJson(refresh1), 401);

        // logout kills the new token
        send("/api/auth/logout", refreshJson(refresh2), 204);
        send("/api/auth/refresh", refreshJson(refresh2), 401);

        // logout with garbage is still 204
        send("/api/auth/logout", refreshJson("garbage"), 204);
    }

    @Test
    void invalidAccessTokenGets401() throws Exception {
        mockMvc.perform(get("/api/orders").header("Authorization", "Bearer abc"))
                .andExpect(status().isUnauthorized());
    }

    private String send(String url, String json, int expectedStatus) throws Exception {
        return mockMvc.perform(post(url)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().is(expectedStatus))
                .andReturn().getResponse().getContentAsString();
    }

    private String field(String body, String jsonPath) {
        return JsonPath.read(body, jsonPath);
    }

    private String refreshJson(String token) {
        return """
                {"refreshToken":"%s"}
                """.formatted(token);
    }
}
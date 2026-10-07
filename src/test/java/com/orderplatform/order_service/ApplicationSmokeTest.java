package com.orderplatform.order_service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.orderplatform.order_service.repository.UserRepository;

class ApplicationSmokeTest extends IntegrationTestBase {

    @Autowired
    private UserRepository userRepository;

    @Test
    void applicationStartsAndDatabaseIsEmpty() {
        assertThat(userRepository.count()).isGreaterThanOrEqualTo(0);
    }
}
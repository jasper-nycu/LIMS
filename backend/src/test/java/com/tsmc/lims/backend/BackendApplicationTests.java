package com.tsmc.lims.backend;

import com.tsmc.lims.backend.support.PostgresTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest
class BackendApplicationTests {

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        PostgresTestSupport.configure(registry, "lims_test_smoke");
    }

    @Test
    void contextLoads() {
    }
}
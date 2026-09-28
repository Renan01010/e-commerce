package com.techstore.cart.adapter.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.techstore.cart.application.port.out.CartStorePort;
import com.techstore.cart.domain.CartItem;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class CartConcurrentAddIntegrationTest {
    private static final int WORKERS = 8;
    private static final int ADDS_PER_WORKER = 5;

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");

    @Autowired
    private CartStorePort cartStore;

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("techstore.jwt.secret", () -> "integration-test-secret-at-least-thirty-two-bytes");
    }

    @Test
    void concurrentAddsKeepOneLineAndSumAllQuantities() throws Exception {
        UUID ownerUserId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(WORKERS);
        try {
            List<Future<?>> futures = new ArrayList<>(WORKERS);
            for (int worker = 0; worker < WORKERS; worker++) {
                futures.add(executor.submit(() -> {
                        start.await();
                        for (int add = 0; add < ADDS_PER_WORKER; add++) {
                            cartStore.add(ownerUserId, productId, 1);
                        }
                        return null;
                    }));
            }
            start.countDown();
            for (Future<?> future : futures) future.get(Duration.ofSeconds(20).toMillis(), java.util.concurrent.TimeUnit.MILLISECONDS);

            List<CartItem> items = cartStore.findByOwner(ownerUserId);
            assertEquals(1, items.size());
            assertEquals(WORKERS * ADDS_PER_WORKER, items.getFirst().quantity());
        } finally {
            executor.shutdownNow();
        }
    }
}
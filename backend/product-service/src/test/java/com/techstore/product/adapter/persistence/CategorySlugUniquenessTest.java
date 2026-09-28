package com.techstore.product.adapter.persistence;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.techstore.product.application.port.CategoryRepository;
import com.techstore.product.domain.Category;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(CategoryPersistenceAdapter.class)
@Testcontainers(disabledWithoutDocker = true)
class CategorySlugUniquenessTest {
    private static final UUID ACTIVE_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440001");
    private static final UUID INACTIVE_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440002");

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");

    @Autowired
    private CategoryRepository categories;

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Test
    void slugUniquenessQueriesIncludeInactiveRowsAndIgnoreTheCurrentId() {
        LocalDateTime now = LocalDateTime.parse("2026-09-28T12:00:00");
        categories.save(new Category(ACTIVE_ID, "Audio", "audio", null, null, 0, true,
                now, now, "admin", "admin"));
        categories.save(new Category(INACTIVE_ID, "Legacy Audio", "legacy-audio", null, null, 0, false,
                now, now, "admin", "admin"));

        assertTrue(categories.existsBySlug("audio"));
        assertTrue(categories.existsBySlug("legacy-audio"));
        assertTrue(categories.existsBySlugAndIdNot("legacy-audio", ACTIVE_ID));
        assertFalse(categories.existsBySlugAndIdNot("legacy-audio", INACTIVE_ID));
        assertFalse(categories.existsBySlug("missing-slug"));
    }
}

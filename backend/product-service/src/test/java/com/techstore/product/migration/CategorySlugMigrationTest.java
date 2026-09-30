package com.techstore.product.migration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.SQLException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.flywaydb.core.api.FlywayException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
class CategorySlugMigrationTest {
    private static final UUID CATEGORY_ONE = UUID.fromString("550e8400-e29b-41d4-a716-446655440001");
    private static final UUID CATEGORY_TWO = UUID.fromString("550e8400-e29b-41d4-a716-446655440002");
    private static final UUID PRODUCT_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440010");

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");

    private String schema;

    @BeforeEach
    void createIsolatedSchemaAndApplyOnlyV1() throws Exception {
        schema = "category_slug_" + UUID.randomUUID().toString().replace("-", "");
        try (Connection connection = connection(); Statement statement = connection.createStatement()) {
            statement.execute("CREATE SCHEMA " + schema);
        }
        flyway("", MigrationVersion.fromVersion("1")).migrate();
        insertLegacyCategories();
    }

    @AfterEach
    void dropIsolatedSchema() throws Exception {
        if (schema == null) return;
        try (Connection connection = connection(); Statement statement = connection.createStatement()) {
            statement.execute("DROP SCHEMA IF EXISTS " + schema + " CASCADE");
        }
    }

    @Test
    void abortsBeforeWritingWhenLegacyNamesCollideAndNoOverrideMapIsProvided() throws Exception {
        FlywayException failure = assertThrows(FlywayException.class, () -> flyway("", null).migrate());

        String diagnostics = Stream.iterate(failure, current -> current != null, Throwable::getCause)
                .map(Throwable::getMessage)
                .filter(message -> message != null)
                .collect(Collectors.joining("\n"));
        assertTrue(diagnostics.contains(CATEGORY_ONE.toString()));
        assertTrue(diagnostics.contains(CATEGORY_TWO.toString()));
        assertTrue(diagnostics.contains("cafe-cha"));
        assertFalse(columnExists("slug"));
        assertEquals(2, count("categories"));
        assertEquals(CATEGORY_TWO, productCategory());
    }

    @Test
    void rejectsIncompleteOverrideMapWithoutWritingAnySlug() throws Exception {
        String incompleteMap = "{\"" + CATEGORY_ONE + "\":\"cafe-cha\"}";

        FlywayException failure = assertThrows(FlywayException.class,
                () -> flyway(incompleteMap, null).migrate());

        String diagnostics = Stream.iterate(failure, current -> current != null, Throwable::getCause)
                .map(Throwable::getMessage)
                .filter(message -> message != null)
                .collect(Collectors.joining("\n"));
        assertTrue(diagnostics.contains(CATEGORY_TWO.toString()));
        assertFalse(columnExists("slug"));
        assertEquals(2, count("categories"));
        assertEquals(CATEGORY_TWO, productCategory());
    }

    @Test
    void rejectsNonStringOverrideValuesBeforeWritingAnySlug() throws Exception {
        String invalidMap = "{\"" + CATEGORY_ONE + "\":123}";

        assertThrows(FlywayException.class, () -> flyway(invalidMap, null).migrate());
        assertFalse(columnExists("slug"));
        assertEquals(2, count("categories"));
    }

    @Test
    void appliesCompleteExplicitMapAndPreservesCategoryAndProductRelationships() throws Exception {
        String completeMap = "{\"" + CATEGORY_ONE + "\":\"cafe-cha\",\""
                + CATEGORY_TWO + "\":\"cafe-cha-legado\"}";

        flyway(completeMap, null).migrate();

        assertTrue(columnExists("slug"));
        assertEquals("cafe-cha", slug(CATEGORY_ONE));
        assertEquals("cafe-cha-legado", slug(CATEGORY_TWO));
        assertEquals("Café & Chá", categoryName(CATEGORY_ONE));
        assertEquals("Cafe-Cha", categoryName(CATEGORY_TWO));
        assertEquals(CATEGORY_ONE, parentCategory(CATEGORY_TWO));
        assertFalse(categoryIsActive(CATEGORY_TWO));
        assertEquals(PRODUCT_ID, productId());
        assertEquals(CATEGORY_TWO, productCategory());
        assertEquals(2, count("categories"));
        assertEquals(1, count("products"));
        assertThrows(SQLException.class, () -> updateSlug(CATEGORY_ONE, "cafe-cha-legado"));
    }

    private Flyway flyway(String overridesJson, MigrationVersion target) {
        var configuration = Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .schemas(schema)
                .defaultSchema(schema)
                .locations("classpath:db/migration")
                .placeholders(Map.of("categorySlugOverrides", overridesJson));
        if (target != null) configuration.target(target);
        return configuration.load();
    }

    private Connection connection() throws Exception {
        return DriverManager.getConnection(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
    }

    private void insertLegacyCategories() throws Exception {
        try (Connection connection = connection(); Statement statement = connection.createStatement()) {
            statement.execute("SET search_path TO " + schema);
            try (PreparedStatement insert = connection.prepareStatement("""
                    INSERT INTO categories
                    (id, name, parent_category_id, display_order, is_active, created_at, updated_at, created_by, updated_by)
                    VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'migration-test', 'migration-test')
                    """)) {
                insert.setObject(1, CATEGORY_ONE);
                insert.setString(2, "Café & Chá");
                insert.setObject(3, null);
                insert.setInt(4, 0);
                insert.setBoolean(5, true);
                insert.executeUpdate();

                insert.setObject(1, CATEGORY_TWO);
                insert.setString(2, "Cafe-Cha");
                insert.setObject(3, CATEGORY_ONE);
                insert.setInt(4, 1);
                insert.setBoolean(5, false);
                insert.executeUpdate();
            }
            try (PreparedStatement insertProduct = connection.prepareStatement("""
                    INSERT INTO products
                    (id, name, price, sku, category_id, quantity, created_at, updated_at, created_by, updated_by)
                    VALUES (?, 'Legacy Product', 9.99, 'LEGACY-1', ?, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP,
                            'migration-test', 'migration-test')
                    """)) {
                insertProduct.setObject(1, PRODUCT_ID);
                insertProduct.setObject(2, CATEGORY_TWO);
                insertProduct.executeUpdate();
            }
        }
    }

    private boolean columnExists(String column) throws Exception {
        try (Connection connection = connection(); PreparedStatement query = connection.prepareStatement("""
                SELECT COUNT(*) FROM information_schema.columns
                WHERE table_schema = ? AND table_name = 'categories' AND column_name = ?
                """)) {
            query.setString(1, schema);
            query.setString(2, column);
            try (ResultSet result = query.executeQuery()) {
                result.next();
                return result.getInt(1) > 0;
            }
        }
    }

    private int count(String table) throws Exception {
        try (Connection connection = connection(); Statement statement = connection.createStatement()) {
            statement.execute("SET search_path TO " + schema);
            try (ResultSet result = statement.executeQuery("SELECT COUNT(*) FROM " + table)) {
                result.next();
                return result.getInt(1);
            }
        }
    }

    private String slug(UUID categoryId) throws Exception {
        try (Connection connection = connection()) {
            connection.setSchema(schema);
            try (PreparedStatement query = connection.prepareStatement("SELECT slug FROM categories WHERE id = ?")) {
                query.setObject(1, categoryId);
                try (ResultSet result = query.executeQuery()) {
                    result.next();
                    return result.getString(1);
                }
            }
        }
    }

    private String categoryName(UUID categoryId) throws Exception {
        try (Connection connection = connection()) {
            connection.setSchema(schema);
            try (PreparedStatement query = connection.prepareStatement("SELECT name FROM categories WHERE id = ?")) {
                query.setObject(1, categoryId);
                try (ResultSet result = query.executeQuery()) {
                    result.next();
                    return result.getString(1);
                }
            }
        }
    }

    private UUID parentCategory(UUID categoryId) throws Exception {
        try (Connection connection = connection()) {
            connection.setSchema(schema);
            try (PreparedStatement query = connection.prepareStatement("SELECT parent_category_id FROM categories WHERE id = ?")) {
                query.setObject(1, categoryId);
                try (ResultSet result = query.executeQuery()) {
                    result.next();
                    return result.getObject(1, UUID.class);
                }
            }
        }
    }

    private boolean categoryIsActive(UUID categoryId) throws Exception {
        try (Connection connection = connection()) {
            connection.setSchema(schema);
            try (PreparedStatement query = connection.prepareStatement("SELECT is_active FROM categories WHERE id = ?")) {
                query.setObject(1, categoryId);
                try (ResultSet result = query.executeQuery()) {
                    result.next();
                    return result.getBoolean(1);
                }
            }
        }
    }

    private void updateSlug(UUID categoryId, String slug) throws Exception {
        try (Connection connection = connection()) {
            connection.setSchema(schema);
            try (PreparedStatement update = connection.prepareStatement("UPDATE categories SET slug = ? WHERE id = ?")) {
                update.setString(1, slug);
                update.setObject(2, categoryId);
                update.executeUpdate();
            }
        }
    }

    private UUID productId() throws Exception {
        try (Connection connection = connection(); Statement statement = connection.createStatement()) {
            statement.execute("SET search_path TO " + schema);
            try (ResultSet result = statement.executeQuery("SELECT id FROM products")) {
                result.next();
                return result.getObject(1, UUID.class);
            }
        }
    }

    private UUID productCategory() throws Exception {
        try (Connection connection = connection(); Statement statement = connection.createStatement()) {
            statement.execute("SET search_path TO " + schema);
            try (ResultSet result = statement.executeQuery("SELECT category_id FROM products")) {
                result.next();
                return result.getObject(1, UUID.class);
            }
        }
    }
}

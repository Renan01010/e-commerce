package com.techstore.cart.adapter.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.sql.DriverManager;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
class CartPriceSnapshotMigrationTest {
    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");

    @Test
    void addsSnapshotColumnsWithoutChangingV1AndLeavesLegacyPriceUnknown() throws Exception {
        Flyway v1Flyway = flyway().target("1").load();
        v1Flyway.migrate();
        Integer originalV1Checksum = checksumFor(v1Flyway.info().applied(), "1");

        UUID ownerId = UUID.randomUUID();
        UUID activeProductId = UUID.randomUUID();
        UUID unavailableProductId = UUID.randomUUID();
        try (var connection = DriverManager.getConnection(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
             var statement = connection.prepareStatement(
                     "insert into cart_items (owner_user_id, product_id, quantity) values (?, ?, ?)")) {
            for (UUID productId : new UUID[] {activeProductId, unavailableProductId}) {
                statement.setObject(1, ownerId);
                statement.setObject(2, productId);
                statement.setInt(3, 2);
                statement.addBatch();
            }
            statement.executeBatch();
        }

        Flyway latestFlyway = flyway().load();
        latestFlyway.migrate();
        latestFlyway.validate();

        Integer migratedV1Checksum = checksumFor(latestFlyway.info().applied(), "1");
        assertEquals(originalV1Checksum, migratedV1Checksum);
        assertNotNull(checksumFor(latestFlyway.info().applied(), "2"));

        try (var connection = DriverManager.getConnection(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
             var statement = connection.prepareStatement(
                     "select price_snapshot_status, unit_price from cart_items where owner_user_id = ?")) {
            statement.setObject(1, ownerId);
            try (var rows = statement.executeQuery()) {
                int migratedRows = 0;
                while (rows.next()) {
                    assertEquals("PENDING", rows.getString("price_snapshot_status"));
                    assertNull(rows.getBigDecimal("unit_price"));
                    migratedRows++;
                }
                assertEquals(2, migratedRows);
            }
        }
    }

    private static FluentConfiguration flyway() {
        return Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration");
    }

    private static Integer checksumFor(MigrationInfo[] migrations, String version) {
        return java.util.Arrays.stream(migrations)
                .filter(migration -> version.equals(migration.getVersion().getVersion()))
                .map(MigrationInfo::getChecksum)
                .findFirst()
                .orElse(null);
    }
}

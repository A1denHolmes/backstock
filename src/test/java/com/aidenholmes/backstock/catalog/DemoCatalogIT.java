package com.aidenholmes.backstock.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import com.aidenholmes.backstock.BackstockApplication;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.postgresql.PostgreSQLContainer;

class DemoCatalogIT {

    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:18");
    private static final AtomicInteger DATABASE_SEQUENCE = new AtomicInteger();

    static {
        POSTGRES.start();
    }

    @Test
    void localProfileSeedsExpectedCatalog() {
        try (ConfigurableApplicationContext context = startApplication("local", createTestDatabase())) {
            JdbcTemplate jdbcTemplate = context.getBean(JdbcTemplate.class);

            String categoriesQuery = "SELECT slug FROM categories";
            List<String> categories = jdbcTemplate.queryForList(categoriesQuery, String.class);

            String productsQuery = """
                SELECT p.sku, c.slug AS category_slug
                FROM products p
                JOIN categories c ON c.id = p.category_id
            """;
            List<ProductWithCategory> products = jdbcTemplate.query(
                    productsQuery,
                    (rs, rowNum) -> new ProductWithCategory(rs.getString("sku"), rs.getString("category_slug")));

            assertThat(categories).containsExactlyInAnyOrder("demo-books", "demo-keyboards", "demo-desk-accessories");
            assertThat(products)
                    .containsExactlyInAnyOrder(
                            new ProductWithCategory("DEMO-BOOK-EFFECTIVE-JAVA", "demo-books"),
                            new ProductWithCategory("DEMO-BOOK-PRAGMATIC", "demo-books"),
                            new ProductWithCategory("DEMO-BOOK-DDIA", "demo-books"),
                            new ProductWithCategory("DEMO-KB-COMPACT", "demo-keyboards"),
                            new ProductWithCategory("DEMO-KB-WIRELESS", "demo-keyboards"),
                            new ProductWithCategory("DEMO-KB-MECHANICAL", "demo-keyboards"),
                            new ProductWithCategory("DEMO-DESK-CABLE-CLIPS", "demo-desk-accessories"),
                            new ProductWithCategory("DEMO-DESK-MAT", "demo-desk-accessories"),
                            new ProductWithCategory("DEMO-DESK-LAPTOP-STAND", "demo-desk-accessories"),
                            new ProductWithCategory("DEMO-DESK-MONITOR-ARM", "demo-desk-accessories"));
        }
    }

    @Test
    void repeatedMigrationPreservesCatalogChanges() {
        try (ConfigurableApplicationContext context = startApplication("local", createTestDatabase())) {
            JdbcTemplate jdbcTemplate = context.getBean(JdbcTemplate.class);

            String adjustStockQuery = """
                UPDATE products
                SET stock_quantity = 0
                WHERE sku = 'DEMO-KB-MECHANICAL' AND stock_quantity <> 0
            """;
            assertThat(jdbcTemplate.update(adjustStockQuery)).isEqualTo(1);
            String renameCategoryQuery = """
                UPDATE categories
                SET name = 'Programming Books'
                WHERE slug = 'demo-books'
            """;
            assertThat(jdbcTemplate.update(renameCategoryQuery)).isEqualTo(1);
            String deactivateProductQuery = """
                UPDATE products
                SET is_active = false
                WHERE sku = 'DEMO-KB-COMPACT'
            """;
            assertThat(jdbcTemplate.update(deactivateProductQuery)).isEqualTo(1);
            String insertProductQuery = """
                INSERT INTO products (name, sku, price, stock_quantity, category_id, created_at, updated_at)
                SELECT 'USB-C Hub', 'CUSTOM-USB-HUB', 29.90, 3, id, current_timestamp, current_timestamp
                FROM categories
                WHERE slug = 'demo-desk-accessories'
            """;
            assertThat(jdbcTemplate.update(insertProductQuery)).isEqualTo(1);

            var categoriesBefore = jdbcTemplate.queryForList("SELECT * FROM categories ORDER BY slug");
            var productsBefore = jdbcTemplate.queryForList("SELECT * FROM products ORDER BY sku");

            context.getBean(Flyway.class).migrate();

            assertThat(jdbcTemplate.queryForList("SELECT * FROM categories ORDER BY slug"))
                    .isEqualTo(categoriesBefore);
            assertThat(jdbcTemplate.queryForList("SELECT * FROM products ORDER BY sku"))
                    .isEqualTo(productsBefore);
        }
    }

    @Test
    void partialCatalogSeedsOnlyProductsWithActiveCategories() {
        try (ConfigurableApplicationContext context = startApplication("local", createTestDatabase())) {
            JdbcTemplate jdbcTemplate = context.getBean(JdbcTemplate.class);

            String deleteProductsQuery = """
                DELETE FROM products
                WHERE sku
                IN ('DEMO-BOOK-DDIA', 'DEMO-KB-WIRELESS')
            """;
            assertThat(jdbcTemplate.update(deleteProductsQuery)).isEqualTo(2);
            String deactivateCategoryQuery = """
                UPDATE categories
                SET is_active = false
                WHERE slug = 'demo-keyboards'
            """;
            assertThat(jdbcTemplate.update(deactivateCategoryQuery)).isEqualTo(1);

            context.getBean(Flyway.class).migrate();

            String productsQuery = """
                SELECT p.sku, c.slug AS category_slug
                FROM products p
                JOIN categories c ON c.id = p.category_id
                WHERE p.sku IN ('DEMO-BOOK-DDIA', 'DEMO-KB-WIRELESS')
            """;
            assertThat(jdbcTemplate.query(
                            productsQuery,
                            (rs, rowNum) ->
                                    new ProductWithCategory(rs.getString("sku"), rs.getString("category_slug"))))
                    .containsExactly(new ProductWithCategory("DEMO-BOOK-DDIA", "demo-books"));
            String categoryActivityQuery = """
                SELECT is_active
                FROM categories
                WHERE slug = 'demo-keyboards'
            """;
            assertThat(jdbcTemplate.queryForObject(categoryActivityQuery, Boolean.class))
                    .isFalse();
        }
    }

    @Test
    void testProfileMigratesWithoutDemoData() {
        try (ConfigurableApplicationContext context = startApplication("test", createTestDatabase())) {
            JdbcTemplate jdbcTemplate = context.getBean(JdbcTemplate.class);

            assertThat(jdbcTemplate.queryForList("SELECT slug FROM categories", String.class))
                    .isEmpty();
            assertThat(jdbcTemplate.queryForList("SELECT sku FROM products", String.class))
                    .isEmpty();
        }
    }

    @Test
    void switchingFromLocalToTestPreservesDataAndValidatesHistory() {
        String jdbcUrl = createTestDatabase();
        String productIdsQuery = "SELECT sku, id FROM products ORDER BY sku";
        List<Map<String, Object>> expectedProducts;

        try (ConfigurableApplicationContext context = startApplication("local", jdbcUrl)) {
            JdbcTemplate jdbcTemplate = context.getBean(JdbcTemplate.class);
            String deleteWirelessKeyboardQuery = """
                DELETE FROM products WHERE sku = 'DEMO-KB-WIRELESS'
            """;
            assertThat(jdbcTemplate.update(deleteWirelessKeyboardQuery)).isEqualTo(1);
            expectedProducts = jdbcTemplate.queryForList(productIdsQuery);
            assertThat(expectedProducts).hasSize(9);
        }

        try (ConfigurableApplicationContext context = startApplication("test", jdbcUrl)) {
            context.getBean(Flyway.class).validate();
            JdbcTemplate jdbcTemplate = context.getBean(JdbcTemplate.class);

            assertThat(jdbcTemplate.queryForList(productIdsQuery)).isEqualTo(expectedProducts);
        }
    }

    private ConfigurableApplicationContext startApplication(String profile, String jdbcUrl) {
        SpringApplicationBuilder builder =
                new SpringApplicationBuilder(BackstockApplication.class).web(WebApplicationType.NONE);

        return builder.run(
                "--spring.profiles.active=" + profile,
                "--spring.datasource.url=" + jdbcUrl,
                "--spring.datasource.username=" + POSTGRES.getUsername(),
                "--spring.datasource.password=" + POSTGRES.getPassword());
    }

    private String createTestDatabase() {
        String jdbcUrl = POSTGRES.getJdbcUrl();
        try (var conn = DriverManager.getConnection(jdbcUrl, POSTGRES.getUsername(), POSTGRES.getPassword())) {
            String name = "demo_" + DATABASE_SEQUENCE.incrementAndGet();
            String sql = "CREATE DATABASE " + name;
            try (Statement statement = conn.createStatement()) {
                statement.execute(sql);
                return jdbcUrl.substring(0, jdbcUrl.lastIndexOf('/') + 1) + name;
            }
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    private record ProductWithCategory(String sku, String categorySlug) {}
}

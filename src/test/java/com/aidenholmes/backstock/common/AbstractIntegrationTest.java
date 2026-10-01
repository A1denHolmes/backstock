package com.aidenholmes.backstock.common;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest
public abstract class AbstractIntegrationTest extends PostgresContainerSupport {

    private static final String SELECT_TABLES = """
            SELECT quote_ident(table_name)
            FROM information_schema.tables
            WHERE table_schema = current_schema()
                AND table_type = 'BASE TABLE'
                AND table_name <> 'flyway_schema_history'
            """;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanDatabase() {
        List<String> tables = jdbcTemplate.queryForList(SELECT_TABLES, String.class);
        if (!tables.isEmpty()) {
            jdbcTemplate.execute("TRUNCATE TABLE " + String.join(", ", tables));
        }
    }
}

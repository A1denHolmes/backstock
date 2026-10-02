package com.aidenholmes.backstock;

import static org.assertj.core.api.Assertions.assertThat;

import com.aidenholmes.backstock.common.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

class ApplicationContextIT extends AbstractIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void connectsToDatabase() {
        Integer actual = jdbcTemplate.queryForObject("SELECT 1", Integer.class);

        assertThat(actual).isEqualTo(1);
    }
}

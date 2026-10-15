package com.aidenholmes.backstock.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import com.aidenholmes.backstock.common.PostgresContainerSupport;
import com.aidenholmes.backstock.config.JpaAuditingConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

/**
 * Uses {@link Category} only to violate a unique constraint. The checked setting applies to every database connection,
 * so this test covers the whole application.
 */
@DataJpaTest
@Import(JpaAuditingConfig.class)
@ExtendWith(OutputCaptureExtension.class)
class DatabaseErrorLoggingIT extends PostgresContainerSupport {

    @Autowired
    private CategoryRepository repository;

    @Test
    void parameterValuesAreNotLoggedOnConstraintViolation(CapturedOutput output) {
        repository.saveAndFlush(new Category("name-leak-marker", "slug-leak-marker"));

        assertThatExceptionOfType(DataIntegrityViolationException.class)
                .isThrownBy(() -> repository.saveAndFlush(new Category("name-leak-marker", "slug-leak-marker")));
        assertThat(output)
                .contains("categories_slug_key")
                .doesNotContain("name-leak-marker")
                .doesNotContain("slug-leak-marker");
    }
}

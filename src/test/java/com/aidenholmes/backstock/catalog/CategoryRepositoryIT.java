package com.aidenholmes.backstock.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.aidenholmes.backstock.common.PostgresContainerSupport;
import com.aidenholmes.backstock.config.JpaAuditingConfig;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

@DataJpaTest
@Import(JpaAuditingConfig.class)
class CategoryRepositoryIT extends PostgresContainerSupport {

    @Autowired
    private CategoryRepository repository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void idIsAssignedWhenCategoryIsSaved() {
        Category category = repository.save(new Category("Books", "books"));

        assertThat(category.getId()).isNotNull();
    }

    @Test
    void categoryIsFoundBySlug() {
        repository.saveAndFlush(new Category("Books", "books"));
        entityManager.clear();

        Optional<Category> found = repository.findBySlug("books");

        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Books");
        assertThat(found.get().isActive()).isTrue();
    }

    @Test
    void unknownSlugIsNotFound() {
        repository.saveAndFlush(new Category("Books", "books"));

        Optional<Category> found = repository.findBySlug("phones");

        assertThat(found).isEmpty();
    }

    @Test
    void duplicateSlugCannotBeStored() {
        repository.saveAndFlush(new Category("Books", "books"));

        assertThatThrownBy(() -> repository.saveAndFlush(new Category("Other", "books")))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("categories_slug_key");
    }

    @Test
    void createdAndUpdatedTimestampsAreFilledAfterPersist() {
        Category category = repository.saveAndFlush(new Category("Books", "books"));

        assertThat(category.getCreatedAt()).isNotNull();
        assertThat(category.getUpdatedAt()).isNotNull();
    }
}
